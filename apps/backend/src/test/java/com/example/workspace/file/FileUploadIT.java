package com.example.workspace.file;

import static com.example.workspace.infrastructure.database.jooq.Tables.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.workspace.common.api.ErrorCode;
import com.example.workspace.common.domain.SourceAccess;
import com.example.workspace.common.exception.AppException;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.file.application.*;
import com.example.workspace.file.dto.FileDtos.*;
import com.example.workspace.infrastructure.storage.*;
import com.example.workspace.support.ApiIT;
import com.example.workspace.workspace.application.CurrentWorkspaceResolver;
import java.io.*;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.*;

@TestPropertySource(properties = {"app.storage.enabled=true", "app.files.max-size=1024", "app.files.quota=1024", "app.jobs.enabled=false"})
class FileUploadIT extends ApiIT {
    @Autowired DSLContext db;
    @Autowired FileUploadService uploads;
    @Autowired CurrentWorkspaceResolver workspaces;
    @Autowired SourceAccess access;
    @Autowired FileCatalog catalog;
    @Autowired FileMaintenance maintenance;
    @Autowired StorageProperties properties;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean ObjectStorage storage;
    @TempDir Path directory;

    @BeforeEach void successfulStorage() {
        doAnswer(call -> {
            ObjectStorage.Put command = call.getArgument(0);
            return new ObjectStorage.Metadata(command.ref(), command.size(), command.contentType(), "etag", command.metadata());
        }).when(storage).put(any(), any());
    }
    private CurrentUser current(RegisteredUser user) {
        return new CurrentUser(UUID.fromString(user.body().path("user").path("id").asText()), user.body().path("user").path("email").asText());
    }
    private UUID workspace(RegisteredUser user) { return UUID.fromString(user.body().path("workspace").path("id").asText()); }
    private CreateUpload request(long size) { return new CreateUpload("test.txt", size, "text/plain", null); }
    private InputStream bytes(String value) { return new ByteArrayInputStream(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }

    @Test void trashRetainsQuotaRestoreFencesExtractionAndPurgeReleasesOnce() throws Exception {
        var user = register(UUID.randomUUID() + "@example.com", "文件生命周期");
        var actor = current(user);
        var upload = uploads.create(actor, "lifecycle", request(5)).upload();
        uploads.receive(actor, upload.uploadId(), bytes("hello"), "text/plain");
        var file = catalog.get(actor, upload.fileId());
        catalog.delete(actor, file.id(), file.version());
        assertThat(db.selectFrom(WORKSPACE_STORAGE_USAGE).where(WORKSPACE_STORAGE_USAGE.WORKSPACE_ID.eq(workspace(user))).fetchSingle().getUsedBytes()).isEqualTo(5);
        assertThatThrownBy(() -> catalog.contentRef(actor, file.id())).isInstanceOf(AppException.class);
        var restored = catalog.restore(actor, file.id(), catalog.get(actor, file.id()).version());
        assertThat(restored.state()).isEqualTo("STORED");
        assertThat(db.selectFrom(FILES).where(FILES.ID.eq(file.id())).fetchSingle().getExtractionGeneration()).isEqualTo(2);
        catalog.delete(actor, file.id(), restored.version());
        db.update(FILES).set(FILES.PURGE_AFTER, OffsetDateTime.now().minusSeconds(1)).where(FILES.ID.eq(file.id())).execute();
        assertThatThrownBy(() -> catalog.restore(actor, file.id(), catalog.get(actor, file.id()).version())).isInstanceOf(AppException.class);
        when(storage.delete(any())).thenThrow(new StorageException(StorageException.Code.TRANSIENT_FAILURE)).thenReturn(ObjectStorage.DeleteResult.DELETED);
        maintenance.purgeFiles();
        var failed = db.selectFrom(BACKGROUND_JOBS).where(BACKGROUND_JOBS.DEDUPE_KEY.eq("purge:" + file.id())).fetchSingle();
        assertThat(failed.getStatus()).isEqualTo("PENDING");
        assertThat(failed.getErrorCode()).isEqualTo("TRANSIENT_FAILURE");
        assertThat(db.selectFrom(WORKSPACE_STORAGE_USAGE).where(WORKSPACE_STORAGE_USAGE.WORKSPACE_ID.eq(workspace(user))).fetchSingle().getUsedBytes()).isEqualTo(5);
        db.update(BACKGROUND_JOBS).set(BACKGROUND_JOBS.NEXT_RUN_AT, OffsetDateTime.now().minusSeconds(1)).where(BACKGROUND_JOBS.ID.eq(failed.getId())).execute();
        maintenance.purgeFiles();
        maintenance.purgeFiles();
        assertThat(db.selectFrom(FILES).where(FILES.ID.eq(file.id())).fetchSingle().getState()).isEqualTo("PURGED");
        assertThat(db.selectFrom(WORKSPACE_STORAGE_USAGE).where(WORKSPACE_STORAGE_USAGE.WORKSPACE_ID.eq(workspace(user))).fetchSingle().getUsedBytes()).isZero();
        verify(storage, times(2)).delete(any());
    }

    @Test void concurrentSameKeyCreatesOneSessionAndReservesOnlyOnce() throws Exception {
        var user = register(UUID.randomUUID() + "@example.com", "上传并发");
        var current = current(user);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<Future<CreatedUpload>>();
            for (int i = 0; i < 8; i++) futures.add(executor.submit(() -> uploads.create(current, "same", request(5))));
            var results = new ArrayList<CreatedUpload>();
            for (var future : futures) results.add(future.get(20, TimeUnit.SECONDS));
            assertThat(results.stream().map(result -> result.upload().uploadId()).distinct().count()).isEqualTo(1);
            assertThat(results.stream().filter(CreatedUpload::created).count()).isEqualTo(1);
        }
        var normalized = uploads.create(current, "same", new CreateUpload("test.txt", 5L, "TEXT/PLAIN; charset=UTF-8", null));
        assertThat(normalized.created()).isFalse();
        assertThat(db.selectFrom(WORKSPACE_STORAGE_USAGE).where(WORKSPACE_STORAGE_USAGE.WORKSPACE_ID.eq(workspace(user))).fetchSingle().getReservedBytes()).isEqualTo(5);
        assertThatThrownBy(() -> uploads.create(current, "same", request(6)))
                .isInstanceOfSatisfying(AppException.class, error -> assertThat(error.code()).isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT));
    }

    @Test void concurrentQuotaClaimsCannotOverbook() throws Exception {
        var user = register(UUID.randomUUID() + "@example.com", "上传配额");
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var results = executor.invokeAll(List.of(
                    () -> reserve(current(user), "quota-a"), () -> reserve(current(user), "quota-b")));
            var counts = new ArrayList<Boolean>();
            for (var result : results) counts.add((Boolean) result.get());
            assertThat(counts).containsExactlyInAnyOrder(true, false);
        }
        assertThat(db.selectFrom(WORKSPACE_STORAGE_USAGE).where(WORKSPACE_STORAGE_USAGE.WORKSPACE_ID.eq(workspace(user))).fetchSingle().getReservedBytes()).isEqualTo(600);
    }
    private boolean reserve(CurrentUser user, String key) {
        try { uploads.create(user, key, request(600)); return true; }
        catch (AppException error) { assertThat(error.code()).isEqualTo(ErrorCode.STORAGE_QUOTA_EXCEEDED); return false; }
    }

    @Test void publishesExactlyOnceAndHidesStorageMetadataFromHttp() throws Exception {
        var user = register(UUID.randomUUID() + "@example.com", "上传发布");
        var other = register(UUID.randomUUID() + "@example.com", "隔离");
        var upload = uploads.create(current(user), "publish", request(5)).upload();
        mockMvc.perform(put(upload.contentPath()).header("Authorization", bearer(user)).contentType("text/plain").content("hello"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.state").value("STORED"))
                .andExpect(jsonPath("$.objectKey").doesNotExist()).andExpect(jsonPath("$.storageBucket").doesNotExist());
        mockMvc.perform(put(upload.contentPath()).header("Authorization", bearer(user)).contentType("text/plain").content("changed"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sizeBytes").value(5));
        mockMvc.perform(get("/api/v1/files/uploads/" + upload.uploadId()).header("Authorization", bearer(other))).andExpect(status().isNotFound());
        verify(storage, times(1)).put(any(), any());
        var usage = db.selectFrom(WORKSPACE_STORAGE_USAGE).where(WORKSPACE_STORAGE_USAGE.WORKSPACE_ID.eq(workspace(user))).fetchSingle();
        assertThat(usage.getUsedBytes()).isEqualTo(5);
        assertThat(usage.getReservedBytes()).isZero();
        assertThat(db.fetchCount(BACKGROUND_JOBS, BACKGROUND_JOBS.DEDUPE_KEY.eq("extract:" + upload.fileId() + ":1"))).isEqualTo(1);
    }

    @Test void rejectsConcurrentReceiverAndReleasesReservationOnStorageFailure() throws Exception {
        var user = register(UUID.randomUUID() + "@example.com", "上传租约");
        var current = current(user);
        var upload = uploads.create(current, "lease", request(5)).upload();
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        doAnswer(call -> {
            started.countDown();
            if (!release.await(20, TimeUnit.SECONDS)) throw new AssertionError("receiver did not release");
            throw new StorageException(StorageException.Code.TRANSIENT_FAILURE);
        }).when(storage).put(any(), any());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> catchThrowable(() -> uploads.receive(current, upload.uploadId(), bytes("hello"), "text/plain")));
            try {
                assertThat(started.await(20, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> uploads.receive(current, upload.uploadId(), bytes("hello"), "text/plain"))
                        .isInstanceOfSatisfying(AppException.class, error -> assertThat(error.code()).isEqualTo(ErrorCode.UPLOAD_STATE_CONFLICT));
            } finally { release.countDown(); }
            assertThat(first.get(20, TimeUnit.SECONDS)).isInstanceOf(AppException.class);
        }
        assertThat(uploads.get(current, upload.uploadId()).state()).isEqualTo("FAILED");
        var usage = db.selectFrom(WORKSPACE_STORAGE_USAGE).where(WORKSPACE_STORAGE_USAGE.WORKSPACE_ID.eq(workspace(user))).fetchSingle();
        assertThat(usage.getReservedBytes()).isZero();
        assertThat(usage.getUsedBytes()).isZero();
        assertThat(db.selectFrom(FILE_UPLOAD_ATTEMPTS).where(FILE_UPLOAD_ATTEMPTS.UPLOAD_ID.eq(upload.uploadId())).fetchSingle().getState()).isEqualTo("ORPHANED");
    }

    @Test void replacedLeaseCannotPublishOrReleaseNewOwnersReservation() throws Exception {
        var user = register(UUID.randomUUID() + "@example.com", "上传过期");
        var upload = uploads.create(current(user), "lost", request(5)).upload();
        UUID replacement = UUID.randomUUID();
        doAnswer(call -> {
            db.update(FILE_UPLOADS).set(FILE_UPLOADS.LEASE_TOKEN, replacement).where(FILE_UPLOADS.ID.eq(upload.uploadId())).execute();
            ObjectStorage.Put command = call.getArgument(0);
            return new ObjectStorage.Metadata(command.ref(), command.size(), command.contentType(), "etag", command.metadata());
        }).when(storage).put(any(), any());
        assertThatThrownBy(() -> uploads.receive(current(user), upload.uploadId(), bytes("hello"), "text/plain"))
                .isInstanceOfSatisfying(AppException.class, error -> assertThat(error.code()).isEqualTo(ErrorCode.UPLOAD_STATE_CONFLICT));
        var remaining = db.selectFrom(FILE_UPLOADS).where(FILE_UPLOADS.ID.eq(upload.uploadId())).fetchSingle();
        assertThat(remaining.getLeaseToken()).isEqualTo(replacement);
        assertThat(remaining.getReservedBytes()).isEqualTo(5);
        assertThat(db.selectFrom(FILES).where(FILES.ID.eq(upload.fileId())).fetchSingle().getState()).isEqualTo("UPLOADING");
    }

    @Test void committedUploadSurvivesLocalCleanupFailure() throws Exception {
        var user = register(UUID.randomUUID() + "@example.com", "清理故障");
        var upload = uploads.create(current(user), "cleanup", request(5)).upload();
        doAnswer(call -> {
            Path path = call.getArgument(1);
            Files.writeString(path.getParent().resolve("leftover"), "force cleanup failure");
            ObjectStorage.Put command = call.getArgument(0);
            return new ObjectStorage.Metadata(command.ref(), command.size(), command.contentType(), "etag", command.metadata());
        }).when(storage).put(any(), any());
        try (var service = service(transactions)) {
            assertThat(service.receive(current(user), upload.uploadId(), bytes("hello"), "text/plain").file().state()).isEqualTo("STORED");
        }
        assertThat(uploads.get(current(user), upload.uploadId()).state()).isEqualTo("COMPLETED");
    }

    @Test void uncertainDatabaseCommitIsReconciledWithoutDoubleCharging() throws Exception {
        var user = register(UUID.randomUUID() + "@example.com", "提交响应故障");
        var upload = uploads.create(current(user), "commit", request(5)).upload();
        AtomicBoolean armed = new AtomicBoolean(false);
        PlatformTransactionManager uncertain = new PlatformTransactionManager() {
            public TransactionStatus getTransaction(TransactionDefinition definition) { return transactions.getTransaction(definition); }
            public void rollback(TransactionStatus status) { transactions.rollback(status); }
            public void commit(TransactionStatus status) {
                transactions.commit(status);
                if (armed.compareAndSet(true, false)) throw new TransactionSystemException("simulated lost commit acknowledgment");
            }
        };
        doAnswer(call -> {
            armed.set(true);
            ObjectStorage.Put command = call.getArgument(0);
            return new ObjectStorage.Metadata(command.ref(), command.size(), command.contentType(), "etag", command.metadata());
        }).when(storage).put(any(), any());
        try (var service = service(uncertain)) {
            assertThat(service.receive(current(user), upload.uploadId(), bytes("hello"), "text/plain").file().state()).isEqualTo("STORED");
        }
        var usage = db.selectFrom(WORKSPACE_STORAGE_USAGE).where(WORKSPACE_STORAGE_USAGE.WORKSPACE_ID.eq(workspace(user))).fetchSingle();
        assertThat(usage.getUsedBytes()).isEqualTo(5);
        assertThat(usage.getReservedBytes()).isZero();
        assertThat(db.selectFrom(FILE_UPLOAD_ATTEMPTS).where(FILE_UPLOAD_ATTEMPTS.UPLOAD_ID.eq(upload.uploadId())).fetchSingle().getState()).isEqualTo("COMMITTED");
    }

    private TestService service(PlatformTransactionManager manager) {
        return new TestService(manager);
    }
    private final class TestService extends FileUploadService implements AutoCloseable {
        TestService(PlatformTransactionManager manager) {
            super(db, workspaces, access, catalog, storage, properties, new FilePolicy(1024, 1024, 1, directory.toString()), manager);
        }
    }
}
