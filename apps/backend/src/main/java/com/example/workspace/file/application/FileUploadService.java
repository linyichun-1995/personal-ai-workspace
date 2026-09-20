package com.example.workspace.file.application;

import static com.example.workspace.common.domain.SourceAccess.*;
import static com.example.workspace.infrastructure.database.jooq.Tables.*;
import static org.jooq.impl.DSL.currentOffsetDateTime;

import com.example.workspace.common.api.ErrorCode;
import com.example.workspace.common.domain.SourceAccess;
import com.example.workspace.common.exception.AppException;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.common.util.UuidV7;
import com.example.workspace.file.dto.FileDtos.*;
import com.example.workspace.file.dto.FileDtos.File;
import com.example.workspace.infrastructure.database.jooq.tables.records.FileUploadsRecord;
import com.example.workspace.infrastructure.storage.*;
import com.example.workspace.workspace.application.CurrentWorkspaceResolver;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class FileUploadService {
    private final DSLContext db;
    private final CurrentWorkspaceResolver workspaces;
    private final SourceAccess access;
    private final FileCatalog catalog;
    private final ObjectStorage storage;
    private final StorageProperties properties;
    private final FilePolicy policy;
    private final TransactionTemplate tx;
    private final ScheduledExecutorService renewals = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon().name("upload-leases").factory());

    public FileUploadService(DSLContext db, CurrentWorkspaceResolver workspaces, SourceAccess access,
            FileCatalog catalog, ObjectStorage storage, StorageProperties properties, FilePolicy policy,
            PlatformTransactionManager transactions) {
        this.db = db;
        this.workspaces = workspaces;
        this.access = access;
        this.catalog = catalog;
        this.storage = storage;
        this.properties = properties;
        this.policy = policy;
        this.tx = new TransactionTemplate(transactions);
    }

    public CreatedUpload create(CurrentUser user, String key, CreateUpload request) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{1,100}"))
            throw invalid("Idempotency-Key 必须为 1–100 位 ASCII 标识");
        if (request == null || request.size() == null) throw invalid("请声明文件大小");
        String name = FilePolicy.name(request.name());
        String ext = FilePolicy.extension(name);
        FilePolicy.declaredType(ext, request.mediaType());
        if (request.size() < 0) throw invalid("文件大小不能为负数");
        if (request.size() > policy.maxBytes) throw FilePolicy.tooLarge();
        if (!properties.enabled())
            throw FileCatalog.storageFailure(new StorageException(StorageException.Code.INVALID_CONFIGURATION));
        // Equivalent MIME declarations must identify the same upload; never persist arbitrary parameters.
        String media = FilePolicy.FORMATS.get(ext);
        String fingerprint = HexFormat.of().formatHex(Sha256.digest().digest(
                (name + "\n" + request.size() + "\n" + media + "\n" + request.projectId()).getBytes(StandardCharsets.UTF_8)));
        return tx.execute(status -> {
            UUID ws = workspaces.require(user).workspace().id();
            lockWorkspace(ws);
            var prior = db.selectFrom(FILE_UPLOADS).where(FILE_UPLOADS.WORKSPACE_ID.eq(ws))
                    .and(FILE_UPLOADS.IDEMPOTENCY_KEY.eq(key)).fetchOne();
            if (prior != null) {
                if (!fingerprint.equals(prior.getRequestFingerprint()))
                    throw conflict(ErrorCode.IDEMPOTENCY_CONFLICT, "同一上传键对应不同文件参数");
                return new CreatedUpload(dto(prior), false);
            }
            access.requireProject(ws, request.projectId(), true);
            db.insertInto(WORKSPACE_STORAGE_USAGE)
                    .set(WORKSPACE_STORAGE_USAGE.WORKSPACE_ID, ws)
                    .set(WORKSPACE_STORAGE_USAGE.QUOTA_BYTES, policy.quotaBytes)
                    .onConflict(WORKSPACE_STORAGE_USAGE.WORKSPACE_ID).doNothing().execute();
            int reserved = db.update(WORKSPACE_STORAGE_USAGE)
                    .set(WORKSPACE_STORAGE_USAGE.RESERVED_BYTES, WORKSPACE_STORAGE_USAGE.RESERVED_BYTES.add(request.size()))
                    .set(WORKSPACE_STORAGE_USAGE.VERSION, WORKSPACE_STORAGE_USAGE.VERSION.add(1))
                    .set(WORKSPACE_STORAGE_USAGE.UPDATED_AT, currentOffsetDateTime())
                    .where(WORKSPACE_STORAGE_USAGE.WORKSPACE_ID.eq(ws))
                    .and(WORKSPACE_STORAGE_USAGE.USED_BYTES.add(WORKSPACE_STORAGE_USAGE.RESERVED_BYTES)
                            .add(request.size()).le(WORKSPACE_STORAGE_USAGE.QUOTA_BYTES)).execute();
            if (reserved != 1) throw conflict(ErrorCode.STORAGE_QUOTA_EXCEEDED, "空间不足，回收站文件也占用容量");
            UUID file = UuidV7.next(), upload = UuidV7.next();
            db.insertInto(FILES).set(FILES.ID, file).set(FILES.WORKSPACE_ID, ws)
                    .set(FILES.PROJECT_ID, request.projectId()).set(FILES.CREATED_BY, user.userId())
                    .set(FILES.DISPLAY_NAME, name).set(FILES.ORIGINAL_NAME, name)
                    .set(FILES.EXTENSION, ext).set(FILES.SIZE_BYTES, request.size())
                    .set(FILES.STATE, "UPLOADING").execute();
            var row = db.insertInto(FILE_UPLOADS).set(FILE_UPLOADS.ID, upload).set(FILE_UPLOADS.WORKSPACE_ID, ws)
                    .set(FILE_UPLOADS.FILE_ID, file).set(FILE_UPLOADS.IDEMPOTENCY_KEY, key)
                    .set(FILE_UPLOADS.REQUEST_FINGERPRINT, fingerprint).set(FILE_UPLOADS.DECLARED_SIZE, request.size())
                    .set(FILE_UPLOADS.DECLARED_MEDIA_TYPE, media).set(FILE_UPLOADS.RESERVED_BYTES, request.size())
                    .set(FILE_UPLOADS.STATE, "CREATED").set(FILE_UPLOADS.EXPIRES_AT, databaseNow().plusHours(1))
                    .returning().fetchSingle();
            return new CreatedUpload(dto(row), true);
        });
    }

    @Transactional(readOnly = true)
    public Upload get(CurrentUser user, UUID id) {
        return dto(require(workspaces.require(user).workspace().id(), id));
    }

    public record Accepted(File file, boolean created) {}

    public Accepted receive(CurrentUser user, UUID id, InputStream input, String media) {
        var claimed = tx.execute(status -> {
            UUID ws = workspaces.require(user).workspace().id();
            lockWorkspace(ws);
            var upload = require(ws, id);
            var file = catalog.require(ws, upload.getFileId());
            if ("COMPLETED".equals(upload.getState())) return upload;
            access.requireProject(ws, file.get(FILES.PROJECT_ID), true);
            OffsetDateTime now = databaseNow();
            if (List.of("FAILED", "EXPIRED").contains(upload.getState()) || !upload.getExpiresAt().isAfter(now))
                throw conflict(ErrorCode.UPLOAD_STATE_CONFLICT, "上传会话已结束，请重新选择文件上传");
            if (upload.getLeaseUntil() != null && upload.getLeaseUntil().isAfter(now))
                throw conflict(ErrorCode.UPLOAD_STATE_CONFLICT, "该文件正在接收或校验，请查询上传状态");
            if (!"UPLOADING".equals(file.get(FILES.STATE)))
                throw conflict(ErrorCode.UPLOAD_STATE_CONFLICT, "文件状态已改变");
            FilePolicy.declaredType(file.get(FILES.EXTENSION), media);
            UUID attempt = UuidV7.next(), token = UUID.randomUUID();
            String objectKey = "workspaces/" + ws + "/files/" + upload.getFileId() + "/v1/" + attempt;
            // A new attempt never overwrites the candidate left by an expired lease.
            db.update(FILE_UPLOAD_ATTEMPTS).set(FILE_UPLOAD_ATTEMPTS.STATE, "ORPHANED")
                    .set(FILE_UPLOAD_ATTEMPTS.LAST_ERROR_CODE, "LEASE_EXPIRED")
                    .where(FILE_UPLOAD_ATTEMPTS.WORKSPACE_ID.eq(ws)).and(FILE_UPLOAD_ATTEMPTS.UPLOAD_ID.eq(id))
                    .and(FILE_UPLOAD_ATTEMPTS.STATE.in("ALLOCATED", "PUTTING", "VERIFIED")).execute();
            db.insertInto(FILE_UPLOAD_ATTEMPTS).set(FILE_UPLOAD_ATTEMPTS.ID, attempt)
                    .set(FILE_UPLOAD_ATTEMPTS.WORKSPACE_ID, ws).set(FILE_UPLOAD_ATTEMPTS.UPLOAD_ID, id)
                    .set(FILE_UPLOAD_ATTEMPTS.STORAGE_PROFILE_ID, properties.profileId())
                    .set(FILE_UPLOAD_ATTEMPTS.STORAGE_BUCKET, properties.s3().bucket())
                    .set(FILE_UPLOAD_ATTEMPTS.OBJECT_KEY, objectKey).set(FILE_UPLOAD_ATTEMPTS.LEASE_TOKEN, token)
                    .set(FILE_UPLOAD_ATTEMPTS.STATE, "ALLOCATED")
                    .set(FILE_UPLOAD_ATTEMPTS.CLEANUP_AFTER, now.plusHours(2)).execute();
            return db.update(FILE_UPLOADS).set(FILE_UPLOADS.STATE, "RECEIVING")
                    .set(FILE_UPLOADS.CURRENT_ATTEMPT_ID, attempt).set(FILE_UPLOADS.LEASE_TOKEN, token)
                    .set(FILE_UPLOADS.LEASE_UNTIL, now.plusSeconds(90))
                    .where(scope(ws, id)).returning().fetchSingle();
        });
        UUID ws = claimed.getWorkspaceId(), fileId = claimed.getFileId();
        if ("COMPLETED".equals(claimed.getState())) return new Accepted(catalog.get(user, fileId), false);
        UUID token = claimed.getLeaseToken(), attempt = claimed.getCurrentAttemptId();
        AtomicBoolean lost = new AtomicBoolean(false);
        Runnable renew = () -> {
            if (lost.get()) throw leaseLost();
            OffsetDateTime now = databaseNow();
            int changed = db.update(FILE_UPLOADS).set(FILE_UPLOADS.LEASE_UNTIL, now.plusSeconds(90))
                    .where(activeLease(ws, id, token)).execute();
            if (changed != 1) { lost.set(true); throw leaseLost(); }
        };
        var heartbeat = renewals.scheduleAtFixedRate(() -> {
            try { renew.run(); } catch (RuntimeException error) { lost.set(true); }
        }, 20, 20, TimeUnit.SECONDS);
        FilePolicy.TempFile temp = null;
        try {
            String ext = catalog.require(ws, fileId).get(FILES.EXTENSION);
            temp = policy.receive(input, claimed.getDeclaredSize(), ext, renew);
            renew.run();
            final FilePolicy.TempFile received = temp;
            tx.executeWithoutResult(status -> {
                lockWorkspace(ws);
                int changed = db.update(FILE_UPLOADS).set(FILE_UPLOADS.STATE, "VERIFYING")
                        .where(activeLease(ws, id, token)).execute();
                if (changed != 1) throw leaseLost();
                db.update(FILE_UPLOAD_ATTEMPTS).set(FILE_UPLOAD_ATTEMPTS.STATE, "PUTTING")
                        .set(FILE_UPLOAD_ATTEMPTS.SIZE_BYTES, received.size()).set(FILE_UPLOAD_ATTEMPTS.SHA256, received.sha256())
                        .where(FILE_UPLOAD_ATTEMPTS.WORKSPACE_ID.eq(ws)).and(FILE_UPLOAD_ATTEMPTS.ID.eq(attempt)).execute();
            });
            var candidate = db.selectFrom(FILE_UPLOAD_ATTEMPTS).where(FILE_UPLOAD_ATTEMPTS.WORKSPACE_ID.eq(ws))
                    .and(FILE_UPLOAD_ATTEMPTS.ID.eq(attempt)).fetchSingle();
            var ref = new ObjectStorage.Ref(candidate.getStorageProfileId(), candidate.getStorageBucket(), candidate.getObjectKey(), null);
            var stored = storage.put(new ObjectStorage.Put(ref, received.size(), FilePolicy.FORMATS.get(ext), received.sha256(),
                    Map.of("file-id", fileId.toString(), "workspace-id", ws.toString(), "content-version", "1", "sha256", received.sha256())), received.path());
            renew.run();
            File published = tx.execute(status -> {
                workspaces.require(user);
                lockWorkspace(ws);
                var current = require(ws, id);
                if (!token.equals(current.getLeaseToken()) || lost.get() || current.getLeaseUntil() == null
                        || !current.getLeaseUntil().isAfter(databaseNow()) || !current.getExpiresAt().isAfter(databaseNow())
                        || !"VERIFYING".equals(current.getState())) throw leaseLost();
                var currentFile = catalog.require(ws, fileId);
                access.requireProject(ws, currentFile.get(FILES.PROJECT_ID), true);
                if (!"UPLOADING".equals(currentFile.get(FILES.STATE)))
                    throw conflict(ErrorCode.UPLOAD_STATE_CONFLICT, "文件状态已改变");
                db.update(FILES).set(FILES.STATE, "STORED").set(FILES.DETECTED_MEDIA_TYPE, FilePolicy.FORMATS.get(ext))
                        .set(FILES.SHA256, received.sha256()).set(FILES.STORAGE_PROFILE_ID, ref.profileId())
                        .set(FILES.STORAGE_BUCKET, ref.bucket()).set(FILES.OBJECT_KEY, ref.key()).set(FILES.STORAGE_ETAG, stored.etag())
                        .set(FILES.EXTRACTION_STATUS, "QUEUED").set(FILES.EXTRACTION_GENERATION, 1L)
                        .set(FILES.EXTRACTION_REQUESTED_AT, currentOffsetDateTime()).set(FILES.VERSION, FILES.VERSION.add(1))
                        .set(FILES.UPDATED_AT, currentOffsetDateTime()).where(FILES.WORKSPACE_ID.eq(ws)).and(FILES.ID.eq(fileId)).execute();
                db.update(FILE_UPLOAD_ATTEMPTS).set(FILE_UPLOAD_ATTEMPTS.STATE, "COMMITTED")
                        .set(FILE_UPLOAD_ATTEMPTS.VERIFIED_AT, currentOffsetDateTime())
                        .where(FILE_UPLOAD_ATTEMPTS.WORKSPACE_ID.eq(ws)).and(FILE_UPLOAD_ATTEMPTS.ID.eq(attempt)).execute();
                db.update(WORKSPACE_STORAGE_USAGE)
                        .set(WORKSPACE_STORAGE_USAGE.RESERVED_BYTES, WORKSPACE_STORAGE_USAGE.RESERVED_BYTES.sub(current.getReservedBytes()))
                        .set(WORKSPACE_STORAGE_USAGE.USED_BYTES, WORKSPACE_STORAGE_USAGE.USED_BYTES.add(received.size()))
                        .set(WORKSPACE_STORAGE_USAGE.VERSION, WORKSPACE_STORAGE_USAGE.VERSION.add(1))
                        .set(WORKSPACE_STORAGE_USAGE.UPDATED_AT, currentOffsetDateTime())
                        .where(WORKSPACE_STORAGE_USAGE.WORKSPACE_ID.eq(ws)).execute();
                db.update(FILE_UPLOADS).set(FILE_UPLOADS.STATE, "COMPLETED").set(FILE_UPLOADS.RESERVED_BYTES, 0L)
                        .set(FILE_UPLOADS.COMPLETED_AT, currentOffsetDateTime()).setNull(FILE_UPLOADS.LEASE_TOKEN)
                        .setNull(FILE_UPLOADS.LEASE_UNTIL).where(scope(ws, id)).execute();
                db.insertInto(BACKGROUND_JOBS).set(BACKGROUND_JOBS.ID, UuidV7.next()).set(BACKGROUND_JOBS.WORKSPACE_ID, ws)
                        .set(BACKGROUND_JOBS.JOB_TYPE, "EXTRACT_FILE").set(BACKGROUND_JOBS.SOURCE_TYPE, "FILE")
                        .set(BACKGROUND_JOBS.SOURCE_ID, fileId).set(BACKGROUND_JOBS.SOURCE_VERSION, 1L)
                        .set(BACKGROUND_JOBS.GENERATION, 1L).set(BACKGROUND_JOBS.DEDUPE_KEY, "extract:" + fileId + ":1")
                        .onConflict(BACKGROUND_JOBS.DEDUPE_KEY).doNothing().execute();
                return catalog.describe(ws, fileId);
            });
            return new Accepted(published, true);
        } catch (StorageException error) {
            if (recordFailure(ws, id, token, error.code().name(), error)) return new Accepted(catalog.get(user, fileId), false);
            throw FileCatalog.storageFailure(error);
        } catch (AppException error) {
            if (recordFailure(ws, id, token, error.code().name(), error)) return new Accepted(catalog.get(user, fileId), false);
            throw error;
        } catch (IOException error) {
            if (recordFailure(ws, id, token, "RECEIVE_FAILED", error)) return new Accepted(catalog.get(user, fileId), false);
            throw new AppException(ErrorCode.FILE_STORAGE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE, "接收中断，请查询状态后重试");
        } catch (RuntimeException error) {
            // COMMIT may succeed before the database connection reports a failure. Re-read the fact
            // under the same workspace lock before compensating, so a published object stays published.
            if (recordFailure(ws, id, token, "UPLOAD_FAILED", error)) return new Accepted(catalog.get(user, fileId), false);
            throw error;
        } finally {
            heartbeat.cancel(false);
            // Local cleanup is independent of the committed upload result and must not turn success into failure.
            if (temp != null) temp.closeQuietly();
        }
    }

    private boolean recordFailure(UUID ws, UUID id, UUID token, String code, Exception original) {
        try {
            return Boolean.TRUE.equals(tx.execute(status -> {
                lockWorkspace(ws);
                var current = require(ws, id);
                if ("COMPLETED".equals(current.getState())) return true;
                if (!token.equals(current.getLeaseToken())) return false;
                boolean busy = "RATE_LIMITED".equals(code);
                if (!busy) {
                    db.update(WORKSPACE_STORAGE_USAGE)
                            .set(WORKSPACE_STORAGE_USAGE.RESERVED_BYTES, WORKSPACE_STORAGE_USAGE.RESERVED_BYTES.sub(current.getReservedBytes()))
                            .set(WORKSPACE_STORAGE_USAGE.VERSION, WORKSPACE_STORAGE_USAGE.VERSION.add(1))
                            .set(WORKSPACE_STORAGE_USAGE.UPDATED_AT, currentOffsetDateTime())
                            .where(WORKSPACE_STORAGE_USAGE.WORKSPACE_ID.eq(ws)).execute();
                }
                db.update(FILE_UPLOADS).set(FILE_UPLOADS.STATE, busy ? "CREATED" : "FAILED")
                        .set(FILE_UPLOADS.RESERVED_BYTES, busy ? current.getReservedBytes() : 0L)
                        .setNull(FILE_UPLOADS.LEASE_TOKEN).setNull(FILE_UPLOADS.LEASE_UNTIL)
                        .set(FILE_UPLOADS.ERROR_CODE, busy ? null : code).where(scope(ws, id)).execute();
                db.update(FILE_UPLOAD_ATTEMPTS).set(FILE_UPLOAD_ATTEMPTS.STATE, "ORPHANED")
                        .set(FILE_UPLOAD_ATTEMPTS.LAST_ERROR_CODE, code)
                        .where(FILE_UPLOAD_ATTEMPTS.WORKSPACE_ID.eq(ws)).and(FILE_UPLOAD_ATTEMPTS.UPLOAD_ID.eq(id))
                        .and(FILE_UPLOAD_ATTEMPTS.STATE.notIn("COMMITTED", "CLEANED")).execute();
                if (!busy) db.update(FILES).set(FILES.STATE, "FAILED").set(FILES.UPDATED_AT, currentOffsetDateTime())
                        .where(FILES.WORKSPACE_ID.eq(ws)).and(FILES.ID.eq(current.getFileId())).and(FILES.STATE.eq("UPLOADING")).execute();
                return false;
            }));
        } catch (RuntimeException cleanupFailure) {
            // The durable PUTTING candidate remains for lease expiry/reconciliation if the DB is unavailable.
            original.addSuppressed(cleanupFailure);
            return false;
        }
    }

    private Condition scope(UUID ws, UUID id) { return FILE_UPLOADS.WORKSPACE_ID.eq(ws).and(FILE_UPLOADS.ID.eq(id)); }
    private Condition activeLease(UUID ws, UUID id, UUID token) {
        return scope(ws, id).and(FILE_UPLOADS.LEASE_TOKEN.eq(token))
                .and(FILE_UPLOADS.LEASE_UNTIL.gt(currentOffsetDateTime())).and(FILE_UPLOADS.EXPIRES_AT.gt(currentOffsetDateTime()))
                .and(FILE_UPLOADS.STATE.in("RECEIVING", "VERIFYING"));
    }
    private void lockWorkspace(UUID ws) { db.select(WORKSPACES.ID).from(WORKSPACES).where(WORKSPACES.ID.eq(ws)).forUpdate().fetchSingle(); }
    private OffsetDateTime databaseNow() { return db.select(currentOffsetDateTime()).fetchSingle().value1(); }
    private FileUploadsRecord require(UUID ws, UUID id) {
        var row = db.selectFrom(FILE_UPLOADS).where(scope(ws, id)).fetchOne();
        if (row == null) throw missing();
        return row;
    }
    private Upload dto(FileUploadsRecord row) {
        return new Upload(row.getId(), row.getFileId(), row.getState(), row.getExpiresAt().toInstant(),
                "/api/v1/files/uploads/" + row.getId() + "/content", row.getErrorCode());
    }
    private AppException leaseLost() { return conflict(ErrorCode.UPLOAD_STATE_CONFLICT, "上传租约已失效，请查询最终状态"); }
    @PreDestroy public void close() { renewals.shutdownNow(); }
}
