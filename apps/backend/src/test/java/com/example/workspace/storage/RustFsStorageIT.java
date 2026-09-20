package com.example.workspace.storage;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.example.workspace.infrastructure.storage.*;
import com.example.workspace.infrastructure.storage.s3.S3ObjectStorage;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;

/** Opt-in integration suite; creates and cleans only a random private bucket owned by this run. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RustFsStorageIT {
    private S3Client admin;
    private S3ObjectStorage storage;
    private StorageProperties properties;
    private String bucket;
    private URI endpoint;
    @TempDir Path directory;

    @BeforeAll void connect() {
        String access = System.getenv("RUSTFS_TEST_ACCESS_KEY"), secret = System.getenv("RUSTFS_TEST_SECRET_KEY");
        assumeTrue(access != null && secret != null, "RustFS integration credentials not supplied");
        endpoint = URI.create(System.getenv().getOrDefault("RUSTFS_TEST_ENDPOINT", "http://127.0.0.1:9000"));
        bucket = "workspace-it-" + UUID.randomUUID();
        properties = settings(bucket, access, secret);
        admin = S3Client.builder().endpointOverride(endpoint).region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(access, secret)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .httpClientBuilder(ApacheHttpClient.builder())
                .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(30)).apiCallAttemptTimeout(Duration.ofSeconds(10)))
                .build();
        admin.createBucket(b -> b.bucket(bucket));
        storage = new S3ObjectStorage(properties);
    }

    @AfterAll void cleanup() {
        if (storage != null) storage.close();
        if (admin == null) return;
        try {
            // Every key listed here is confined to the freshly-created test bucket.
            for (var page : admin.listObjectsV2Paginator(b -> b.bucket(bucket)))
                for (var object : page.contents()) admin.deleteObject(b -> b.bucket(bucket).key(object.key()));
            admin.deleteBucket(b -> b.bucket(bucket));
        } finally { admin.close(); }
    }

    private StorageProperties settings(String targetBucket, String access, String secret) {
        return new StorageProperties(true, "integration", new StorageProperties.S3(endpoint, "us-east-1", targetBucket,
                access, secret, true, StorageProperties.IntegrityMode.READBACK_SHA256,
                Duration.ofSeconds(3), Duration.ofSeconds(2), Duration.ofSeconds(10), Duration.ofSeconds(15), Duration.ofSeconds(30), 2, 2, List.of()));
    }
    private ObjectStorage.Ref ref(String suffix) { return new ObjectStorage.Ref("integration", bucket, "integration-probes/" + suffix, null); }
    private ObjectStorage.Put put(String suffix, byte[] bytes) throws Exception {
        Path file = Files.write(directory.resolve(UUID.randomUUID().toString()), bytes);
        String digest = Sha256.of(file);
        var command = new ObjectStorage.Put(ref(suffix), bytes.length, "text/plain", digest,
                Map.of("file-id", UUID.randomUUID().toString(), "workspace-id", UUID.randomUUID().toString(), "content-version", "1", "sha256", digest));
        storage.put(command, file);
        return command;
    }

    @Test void exactLengthMetadataReadbackAndPrivateAccess() throws Exception {
        byte[] bytes = "RustFS 中文原始字节\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var command = put("roundtrip", bytes);
        var metadata = storage.head(command.ref());
        assertThat(metadata.size()).isEqualTo(bytes.length);
        assertThat(metadata.metadata()).containsAllEntriesOf(command.metadata());
        assertThat(metadata.contentType()).isEqualTo("text/plain");
        try (var content = storage.open(command.ref())) { assertThat(content.stream().readAllBytes()).isEqualTo(bytes); }
        var anonymous = HttpClient.newHttpClient().send(HttpRequest.newBuilder(endpoint.resolve("/" + bucket + "/" + command.ref().key())).GET().build(), HttpResponse.BodyHandlers.discarding());
        assertThat(anonymous.statusCode()).isIn(401, 403);
        assertThat(storage.probe().writable()).isTrue();
    }

    @Test void listsEveryPageAndDeleteIsIdempotent() throws Exception {
        for (int i = 0; i < 3; i++) put("paging/" + i, new byte[]{65});
        String token = null;
        Set<String> keys = new HashSet<>();
        int pages = 0;
        do {
            var page = storage.list(new ObjectStorage.ListQuery("integration", bucket, "integration-probes/paging/", token, 1));
            assertThat(page.items()).hasSize(1);
            keys.add(page.items().getFirst().ref().key());
            token = page.nextToken();
            assertThat(++pages).isLessThanOrEqualTo(3);
        } while (token != null);
        assertThat(keys).hasSize(3);
        for (var key : keys) {
            var object = new ObjectStorage.Ref("integration", bucket, key, null);
            assertThat(storage.delete(object)).isEqualTo(ObjectStorage.DeleteResult.DELETED);
            assertThat(storage.delete(object)).isEqualTo(ObjectStorage.DeleteResult.ABSENT);
        }
    }

    @Test void abandoningDownloadsReleasesSmallConnectionPool() throws Exception {
        byte[] bytes = new byte[256 * 1024];
        Arrays.fill(bytes, (byte) 65);
        var command = put("abort", bytes);
        for (int i = 0; i < 10; i++) {
            try (var content = storage.open(command.ref())) { assertThat(content.stream().read()).isEqualTo(65); }
        }
        assertThat(storage.head(command.ref()).size()).isEqualTo(bytes.length);
    }

    @Test void separatesMissingObjectMissingBucketAndBadCredentials() {
        assertThatThrownBy(() -> storage.head(ref("absent")))
                .isInstanceOfSatisfying(StorageException.class, error -> assertThat(error.code()).isEqualTo(StorageException.Code.OBJECT_NOT_FOUND));
        var p = properties.s3();
        try (var missing = new AutoClosingStorage(settings("missing-" + UUID.randomUUID(), p.accessKey(), p.secretKey()))) {
            assertThatThrownBy(missing::probe).isInstanceOfSatisfying(StorageException.class,
                    error -> assertThat(error.code()).isEqualTo(StorageException.Code.BUCKET_NOT_FOUND));
        }
        try (var denied = new AutoClosingStorage(settings(bucket, "invalid-" + UUID.randomUUID(), "intentionally-invalid-secret"))) {
            assertThatThrownBy(() -> denied.head(ref("roundtrip"))).isInstanceOfSatisfying(StorageException.class,
                    error -> assertThat(error.code()).isIn(StorageException.Code.AUTHENTICATION_FAILED, StorageException.Code.ACCESS_DENIED));
        }
    }

    @Test void rejectsLocalDigestMismatchAndProviderRejectsIncorrectChecksum() throws Exception {
        Path file = Files.writeString(directory.resolve("checksum"), "integrity");
        assertThatThrownBy(() -> storage.put(new ObjectStorage.Put(ref("wrong-local"), Files.size(file), "text/plain", "0".repeat(64), Map.of()), file))
                .isInstanceOfSatisfying(StorageException.class, error -> assertThat(error.code()).isEqualTo(StorageException.Code.INTEGRITY_MISMATCH));
        assertThatThrownBy(() -> admin.putObject(PutObjectRequest.builder().bucket(bucket).key("integration-probes/wrong-server")
                        .checksumSHA256(Base64.getEncoder().encodeToString(new byte[32])).build(), RequestBody.fromFile(file)))
                .isInstanceOf(S3Exception.class);
        assertThatThrownBy(() -> storage.head(ref("wrong-server"))).isInstanceOfSatisfying(StorageException.class,
                error -> assertThat(error.code()).isEqualTo(StorageException.Code.OBJECT_NOT_FOUND));
    }

    @Test void refusesVersionEnabledAndSuspendedBucketsBeforeWriting() {
        String versioned = "workspace-it-" + UUID.randomUUID();
        admin.createBucket(b -> b.bucket(versioned));
        var p = properties.s3();
        try (var target = new AutoClosingStorage(settings(versioned, p.accessKey(), p.secretKey()))) {
            admin.putBucketVersioning(b -> b.bucket(versioned).versioningConfiguration(c -> c.status(BucketVersioningStatus.ENABLED)));
            assertThatThrownBy(target::probe).isInstanceOfSatisfying(StorageException.class,
                    error -> assertThat(error.code()).isEqualTo(StorageException.Code.INVALID_CONFIGURATION));
            admin.putBucketVersioning(b -> b.bucket(versioned).versioningConfiguration(c -> c.status(BucketVersioningStatus.SUSPENDED)));
            assertThatThrownBy(target::probe).isInstanceOfSatisfying(StorageException.class,
                    error -> assertThat(error.code()).isEqualTo(StorageException.Code.INVALID_CONFIGURATION));
        } finally { admin.deleteBucket(b -> b.bucket(versioned)); }
    }

    @Test void rejectsUnsafeScopeAndRedactsConfiguration() {
        assertThatThrownBy(() -> storage.head(new ObjectStorage.Ref("integration", bucket, "integration-probes/../secret", null)))
                .isInstanceOf(StorageException.class);
        assertThatThrownBy(() -> storage.head(new ObjectStorage.Ref("integration", bucket, "other/key", null)))
                .isInstanceOf(StorageException.class);
        assertThat(properties.toString()).doesNotContain(properties.s3().accessKey(), properties.s3().secretKey());
    }
    private static final class AutoClosingStorage extends S3ObjectStorage implements AutoCloseable {
        AutoClosingStorage(StorageProperties settings) { super(settings); }
    }
}
