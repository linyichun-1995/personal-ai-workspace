package com.example.workspace.storage;

import static org.junit.jupiter.api.Assertions.*;
import com.example.workspace.infrastructure.storage.StorageException.Code;
import com.example.workspace.infrastructure.storage.s3.S3ErrorTranslator;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.s3.model.S3Exception;

class S3ErrorTranslatorTest {
    @Test void absenceDoesNotIncludePermissionsOrBucketFailures() {
        assertEquals(Code.ACCESS_DENIED, S3ErrorTranslator.translate(S3Exception.builder().statusCode(403).build()).code());
        assertEquals(Code.OBJECT_NOT_FOUND, error("NoSuchKey",404));
        assertEquals(Code.BUCKET_NOT_FOUND, error("NoSuchBucket",404));
        assertEquals(Code.AUTHENTICATION_FAILED, error("SignatureDoesNotMatch",403));
        assertEquals(Code.INTEGRITY_MISMATCH, error("BadDigest",400));
        assertEquals(Code.TRANSIENT_FAILURE, error("SlowDown",503));
    }
    private Code error(String code,int status) {
        return S3ErrorTranslator.translate(S3Exception.builder().statusCode(status)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(code).errorMessage("secret must not escape").build()).build()).code();
    }
}
