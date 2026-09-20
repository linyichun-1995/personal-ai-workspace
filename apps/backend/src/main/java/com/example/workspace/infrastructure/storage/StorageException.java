package com.example.workspace.infrastructure.storage;

/** Safe, bounded error vocabulary; supplier messages and credentials do not reach clients. */
public class StorageException extends RuntimeException {
    public enum Code {
        OBJECT_NOT_FOUND, ACCESS_DENIED, AUTHENTICATION_FAILED, BUCKET_NOT_FOUND,
        TRANSIENT_FAILURE, INTEGRITY_MISMATCH, INVALID_CONFIGURATION
    }
    private final Code code;
    private final String providerRequestId;
    public StorageException(Code code) { this(code, null); }
    public StorageException(Code code, String providerRequestId) {
        super(code.name());
        this.code = code;
        this.providerRequestId = providerRequestId;
    }
    public Code code() { return code; }
    public String providerRequestId() { return providerRequestId; }
    public boolean retryable() { return code == Code.TRANSIENT_FAILURE; }
}
