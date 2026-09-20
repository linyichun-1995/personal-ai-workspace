package com.example.workspace.infrastructure.storage;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.storage")
public record StorageProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("rustfs-local") String profileId,
        @DefaultValue S3 s3
) {
    public record S3(
            @DefaultValue("http://127.0.0.1:9000") URI endpoint,
            @DefaultValue("us-east-1") String region,
            @DefaultValue("personal-ai-workspace-dev") String bucket,
            String accessKey, String secretKey,
            @DefaultValue("true") boolean pathStyle,
            @DefaultValue("READBACK_SHA256") IntegrityMode integrityMode,
            @DefaultValue("3s") Duration connectTimeout,
            @DefaultValue("2s") Duration connectionAcquisitionTimeout,
            @DefaultValue("30s") Duration socketTimeout,
            @DefaultValue("45s") Duration attemptTimeout,
            @DefaultValue("120s") Duration callTimeout,
            @DefaultValue("3") int maxAttempts,
            @DefaultValue("32") int maxConnections,
            @DefaultValue List<String> additionalBuckets
    ) {
        @Override public String toString() { return "S3[credentials=REDACTED]"; }
    }
    public enum IntegrityMode { READBACK_SHA256, SERVER_SHA256 }
}
