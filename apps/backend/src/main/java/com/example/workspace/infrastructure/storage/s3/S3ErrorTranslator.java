package com.example.workspace.infrastructure.storage.s3;

import com.example.workspace.infrastructure.storage.StorageException;
import com.example.workspace.infrastructure.storage.StorageException.Code;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.S3Exception;

public final class S3ErrorTranslator {
    private S3ErrorTranslator() {}
    public static StorageException translate(SdkException error) {
        if (error instanceof S3Exception s3) {
            String code = s3.awsErrorDetails() == null ? "" : s3.awsErrorDetails().errorCode();
            Code translated = switch (code == null ? "" : code) {
                case "NoSuchKey", "NotFound" -> Code.OBJECT_NOT_FOUND;
                case "NoSuchBucket" -> Code.BUCKET_NOT_FOUND;
                case "InvalidAccessKeyId", "SignatureDoesNotMatch", "RequestTimeTooSkewed", "ExpiredToken" -> Code.AUTHENTICATION_FAILED;
                case "BadDigest", "InvalidDigest" -> Code.INTEGRITY_MISMATCH;
                default -> s3.statusCode() == 403 ? Code.ACCESS_DENIED
                        : s3.statusCode() == 404 ? Code.OBJECT_NOT_FOUND
                        : s3.statusCode() >= 500 || s3.statusCode() == 429 ? Code.TRANSIENT_FAILURE
                        : Code.INVALID_CONFIGURATION;
            };
            return new StorageException(translated, s3.requestId());
        }
        return new StorageException(Code.TRANSIENT_FAILURE);
    }
}
