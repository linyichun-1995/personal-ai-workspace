package com.example.workspace.infrastructure.storage.s3;

import com.example.workspace.infrastructure.storage.ObjectStorage;
import com.example.workspace.infrastructure.storage.Sha256;
import com.example.workspace.infrastructure.storage.StorageException;
import com.example.workspace.infrastructure.storage.StorageException.Code;
import com.example.workspace.infrastructure.storage.StorageProperties;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

@Component
@EnableConfigurationProperties(StorageProperties.class)
public class S3ObjectStorage implements ObjectStorage {
    private final StorageProperties properties;
    private volatile S3Client client;

    public S3ObjectStorage(StorageProperties properties) { this.properties = properties; }

    // Lazy construction keeps the original workspace available with file storage disabled/misconfigured.
    private synchronized S3Client client() {
        if (!properties.enabled()) throw new StorageException(Code.INVALID_CONFIGURATION);
        if (client != null) return client;
        var p = properties.s3();
        if (p == null || properties.profileId() == null || properties.profileId().isBlank()
                || p.accessKey() == null || p.accessKey().isBlank() || p.secretKey() == null || p.secretKey().isBlank()
                || p.endpoint() == null || !Set.of("http", "https").contains(p.endpoint().getScheme())
                || p.endpoint().getHost() == null || p.endpoint().getUserInfo() != null
                || p.endpoint().getQuery() != null || p.endpoint().getFragment() != null
                || !(p.endpoint().getPath().isEmpty() || p.endpoint().getPath().equals("/"))
                || p.bucket() == null || !p.bucket().matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]")
                || p.region() == null || p.region().isBlank() || p.integrityMode() == null || p.additionalBuckets() == null
                || p.maxAttempts() < 1 || p.maxAttempts() > 3 || p.maxConnections() < 1
                || !positive(p.callTimeout()) || !positive(p.attemptTimeout()) || !positive(p.connectTimeout())
                || !positive(p.connectionAcquisitionTimeout()) || !positive(p.socketTimeout())) {
            throw new StorageException(Code.INVALID_CONFIGURATION);
        }
        try {
            client = S3Client.builder().endpointOverride(p.endpoint()).region(Region.of(p.region()))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(p.accessKey(), p.secretKey())))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(p.pathStyle()).build())
                    .httpClientBuilder(ApacheHttpClient.builder().maxConnections(p.maxConnections())
                            .connectionTimeout(p.connectTimeout()).connectionAcquisitionTimeout(p.connectionAcquisitionTimeout())
                            .socketTimeout(p.socketTimeout()))
                    .overrideConfiguration(c -> c.apiCallTimeout(p.callTimeout()).apiCallAttemptTimeout(p.attemptTimeout())
                            .retryStrategy(StandardRetryStrategy.builder().maxAttempts(p.maxAttempts()).build()))
                    .build();
            return client;
        } catch (IllegalArgumentException e) { throw new StorageException(Code.INVALID_CONFIGURATION); }
    }

    private void validate(Ref ref) {
        if (ref == null || properties.s3() == null || !Objects.equals(properties.profileId(), ref.profileId())
                || ref.bucket() == null || !(Objects.equals(properties.s3().bucket(), ref.bucket())
                    || (properties.s3().additionalBuckets() != null && properties.s3().additionalBuckets().contains(ref.bucket())))
                || ref.versionId() != null || ref.key() == null
                || !(ref.key().startsWith("workspaces/") || ref.key().startsWith("integration-probes/"))
                || ref.key().contains("..") || ref.key().contains("\\") || ref.key().length() > 512
                || ref.key().codePoints().anyMatch(Character::isISOControl)) {
            throw new StorageException(Code.INVALID_CONFIGURATION);
        }
    }
    private boolean positive(java.time.Duration duration) {
        return duration != null && !duration.isNegative() && !duration.isZero();
    }
    private <T> T call(Supplier<T> action) {
        try { return action.get(); }
        catch (SdkException e) { throw S3ErrorTranslator.translate(e); }
    }

    @Override public Metadata put(Put command, Path path) {
        validate(command.ref());
        try {
            if (command.size() < 0 || Files.size(path) != command.size() || command.sha256() == null
                    || !command.sha256().matches("[0-9a-f]{64}") || !Sha256.of(path).equals(command.sha256())) {
                throw new StorageException(Code.INTEGRITY_MISMATCH);
            }
        } catch (IOException e) { throw new StorageException(Code.TRANSIENT_FAILURE); }
        if (!Set.of("file-id", "workspace-id", "content-version", "sha256").containsAll(command.metadata().keySet())
                || command.metadata().values().stream().anyMatch(v -> !v.matches("[\\x20-\\x7E]{1,200}"))
                || command.contentType() == null || !command.contentType().matches("[a-z0-9.+-]+/[a-z0-9.+-]+")
                || (command.metadata().containsKey("sha256") && !command.sha256().equals(command.metadata().get("sha256")))) {
            throw new StorageException(Code.INVALID_CONFIGURATION);
        }
        // A versioned/locked bucket must never receive application bytes.
        probeBucket(command.ref().bucket());
        var request = PutObjectRequest.builder().bucket(command.ref().bucket()).key(command.ref().key())
                .contentLength(command.size()).contentType(command.contentType()).metadata(command.metadata());
        if (properties.s3().integrityMode() == StorageProperties.IntegrityMode.SERVER_SHA256) {
            request.checksumSHA256(Base64.getEncoder().encodeToString(HexFormat.of().parseHex(command.sha256())));
        }
        try {
            call(() -> client().putObject(request.build(), RequestBody.fromFile(path)));
        } catch (StorageException putFailure) {
            if (!putFailure.retryable()) throw putFailure;
            // A lost PUT response does not mean the server lost the bytes. Resolve the existing
            // candidate with HEAD and a complete readback before any business retry chooses a new key.
            try { return verify(command); }
            catch (StorageException verificationFailure) {
                if (verificationFailure.code() == Code.INTEGRITY_MISMATCH) throw verificationFailure;
                throw putFailure;
            }
        }
        return verify(command);
    }

    private Metadata verify(Put command) {
        Metadata stored = head(command.ref());
        if (stored.size() != command.size() || !Objects.equals(stored.contentType(), command.contentType())
                || !stored.metadata().entrySet().containsAll(command.metadata().entrySet()))
            throw new StorageException(Code.INTEGRITY_MISMATCH);
        // Readback remains mandatory until SERVER_SHA256 is independently certified for a deployment.
        try (var content = open(command.ref())) {
            if (!Sha256.of(content.stream()).equals(command.sha256())) throw new StorageException(Code.INTEGRITY_MISMATCH);
        } catch (IOException e) { throw new StorageException(Code.TRANSIENT_FAILURE); }
        return stored;
    }

    @Override public Metadata head(Ref ref) {
        validate(ref);
        try {
            var response = call(() -> client().headObject(b -> b.bucket(ref.bucket()).key(ref.key())));
            if (response.versionId() != null && !response.versionId().equals("null")) throw new StorageException(Code.INVALID_CONFIGURATION);
            return new Metadata(ref, response.contentLength(), response.contentType(), response.eTag(), response.metadata());
        } catch (StorageException e) {
            if (e.code() == Code.OBJECT_NOT_FOUND) headBucket(ref.bucket());
            throw e;
        }
    }

    @Override public Content open(Ref ref) {
        validate(ref);
        var response = call(() -> client().getObject(b -> b.bucket(ref.bucket()).key(ref.key())));
        var r = response.response();
        if (r.versionId() != null && !r.versionId().equals("null")) {
            response.abort();
            throw new StorageException(Code.INVALID_CONFIGURATION);
        }
        return new Content(new Metadata(ref, r.contentLength(), r.contentType(), r.eTag(), r.metadata()), response, response::abort);
    }

    @Override public DeleteResult delete(Ref ref) {
        validate(ref);
        probeBucket(ref.bucket());
        try { head(ref); }
        catch (StorageException e) {
            if (e.code() == Code.OBJECT_NOT_FOUND) return DeleteResult.ABSENT;
            throw e;
        }
        call(() -> client().deleteObject(b -> b.bucket(ref.bucket()).key(ref.key())));
        try { head(ref); }
        catch (StorageException e) {
            if (e.code() == Code.OBJECT_NOT_FOUND) return DeleteResult.DELETED;
            throw e;
        }
        throw new StorageException(Code.TRANSIENT_FAILURE);
    }

    @Override public Page list(ListQuery query) {
        validate(new Ref(query.profileId(), query.bucket(), query.prefix(), null));
        if (query.limit() < 1 || query.limit() > 1000) throw new StorageException(Code.INVALID_CONFIGURATION);
        var page = call(() -> client().listObjectsV2(b -> b.bucket(query.bucket()).prefix(query.prefix())
                .continuationToken(query.token()).maxKeys(query.limit())));
        return new Page(page.contents().stream().map(o -> new Metadata(
                new Ref(query.profileId(), query.bucket(), o.key(), null), o.size(), null, o.eTag(), Map.of())).toList(),
                page.isTruncated() ? page.nextContinuationToken() : null);
    }

    private void headBucket(String bucket) {
        try { call(() -> client().headBucket(b -> b.bucket(bucket))); }
        catch (StorageException e) {
            if (e.code() == Code.OBJECT_NOT_FOUND) throw new StorageException(Code.BUCKET_NOT_FOUND, e.providerRequestId());
            throw e;
        }
    }
    private void probeBucket(String bucket) {
        headBucket(bucket);
        var version = call(() -> client().getBucketVersioning(b -> b.bucket(bucket)));
        if (version.statusAsString() != null && !version.statusAsString().isBlank()) throw new StorageException(Code.INVALID_CONFIGURATION);
        try {
            var lock = client().getObjectLockConfiguration(b -> b.bucket(bucket));
            if (lock.objectLockConfiguration() != null && "Enabled".equals(lock.objectLockConfiguration().objectLockEnabledAsString()))
                throw new StorageException(Code.INVALID_CONFIGURATION);
        } catch (S3Exception e) {
            String code = e.awsErrorDetails() == null ? null : e.awsErrorDetails().errorCode();
            if (!"ObjectLockConfigurationNotFoundError".equals(code) && !"ObjectLockConfigurationNotFound".equals(code))
                throw S3ErrorTranslator.translate(e);
        } catch (SdkException e) { throw S3ErrorTranslator.translate(e); }
    }
    @Override public Probe probe() {
        probeBucket(properties.s3().bucket());
        return new Probe(true, properties.profileId(), properties.s3().bucket());
    }
    @PreDestroy public synchronized void close() { if (client != null) client.close(); }
}
