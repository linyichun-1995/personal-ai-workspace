ALTER TABLE projects ADD CONSTRAINT projects_workspace_id_key UNIQUE (workspace_id, id);
ALTER TABLE tasks ADD CONSTRAINT tasks_workspace_id_key UNIQUE (workspace_id, id);
ALTER TABLE notes ADD CONSTRAINT notes_workspace_id_key UNIQUE (workspace_id, id);

CREATE TABLE workspace_storage_usage (
    workspace_id UUID PRIMARY KEY REFERENCES workspaces(id),
    quota_bytes BIGINT NOT NULL DEFAULT 1073741824 CHECK (quota_bytes >= 0),
    used_bytes BIGINT NOT NULL DEFAULT 0 CHECK (used_bytes >= 0),
    reserved_bytes BIGINT NOT NULL DEFAULT 0 CHECK (reserved_bytes >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (used_bytes + reserved_bytes <= quota_bytes)
);

CREATE TABLE files (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    project_id UUID,
    created_by UUID NOT NULL REFERENCES users(id),
    display_name VARCHAR(200) NOT NULL,
    original_name VARCHAR(200) NOT NULL,
    detected_media_type VARCHAR(100),
    extension VARCHAR(10) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes >= 0),
    sha256 VARCHAR(64),
    storage_provider VARCHAR(10) NOT NULL DEFAULT 'S3' CHECK (storage_provider = 'S3'),
    storage_profile_id VARCHAR(100),
    storage_bucket VARCHAR(63),
    object_key VARCHAR(512),
    storage_etag VARCHAR(200),
    storage_version_id VARCHAR(200),
    content_version BIGINT NOT NULL DEFAULT 1 CHECK (content_version = 1),
    state VARCHAR(20) NOT NULL CHECK (state IN ('UPLOADING','STORED','FAILED','DELETED','PURGED')),
    deleted_at TIMESTAMPTZ,
    deletion_reason VARCHAR(40),
    purge_after TIMESTAMPTZ,
    purged_at TIMESTAMPTZ,
    extraction_status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED'
        CHECK (extraction_status IN ('NOT_STARTED','QUEUED','PROCESSING','READY','EMPTY','SKIPPED','FAILED')),
    extraction_generation BIGINT NOT NULL DEFAULT 0,
    extraction_error_code VARCHAR(50),
    extraction_retryable BOOLEAN NOT NULL DEFAULT FALSE,
    extraction_requested_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    tag_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (workspace_id, id),
    UNIQUE (storage_profile_id, storage_bucket, object_key),
    FOREIGN KEY (workspace_id, project_id) REFERENCES projects(workspace_id, id),
    CHECK (state <> 'STORED' OR (storage_profile_id IS NOT NULL AND storage_bucket IS NOT NULL
        AND object_key IS NOT NULL AND sha256 IS NOT NULL AND detected_media_type IS NOT NULL)),
    CHECK (state NOT IN ('DELETED','PURGED') OR (deleted_at IS NOT NULL AND purge_after IS NOT NULL))
);
CREATE INDEX idx_files_workspace_updated ON files(workspace_id, updated_at DESC, id);
CREATE INDEX idx_files_project_updated ON files(workspace_id, project_id, updated_at DESC, id);
CREATE INDEX idx_files_purge ON files(state, purge_after);
CREATE INDEX idx_files_trash ON files(workspace_id, deleted_at DESC, id);

CREATE TABLE file_uploads (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    file_id UUID NOT NULL UNIQUE,
    idempotency_key VARCHAR(100) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    declared_size BIGINT NOT NULL CHECK (declared_size >= 0),
    declared_media_type VARCHAR(100) NOT NULL,
    reserved_bytes BIGINT NOT NULL CHECK (reserved_bytes >= 0),
    state VARCHAR(20) NOT NULL CHECK (state IN ('CREATED','RECEIVING','VERIFYING','COMPLETED','FAILED','EXPIRED')),
    current_attempt_id UUID,
    lease_token UUID,
    lease_until TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    error_code VARCHAR(50),
    UNIQUE (workspace_id, id),
    UNIQUE (workspace_id, idempotency_key),
    FOREIGN KEY (workspace_id, file_id) REFERENCES files(workspace_id, id)
);
CREATE INDEX idx_upload_expiry ON file_uploads(state, expires_at);

CREATE TABLE file_upload_attempts (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    upload_id UUID NOT NULL,
    storage_profile_id VARCHAR(100) NOT NULL,
    storage_bucket VARCHAR(63) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    lease_token UUID NOT NULL,
    size_bytes BIGINT CHECK (size_bytes >= 0),
    sha256 VARCHAR(64),
    state VARCHAR(20) NOT NULL CHECK (state IN ('ALLOCATED','PUTTING','VERIFIED','COMMITTED','ORPHANED','CLEANED','FAILED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMPTZ,
    cleanup_after TIMESTAMPTZ NOT NULL,
    last_error_code VARCHAR(50),
    UNIQUE (workspace_id, upload_id, id),
    UNIQUE (storage_profile_id, storage_bucket, object_key),
    FOREIGN KEY (workspace_id, upload_id) REFERENCES file_uploads(workspace_id, id)
);
ALTER TABLE file_uploads ADD CONSTRAINT uploads_current_attempt_fk
    FOREIGN KEY (workspace_id, id, current_attempt_id) REFERENCES file_upload_attempts(workspace_id, upload_id, id);
CREATE INDEX idx_attempt_cleanup ON file_upload_attempts(state, cleanup_after);

CREATE TABLE file_extractions (
    workspace_id UUID NOT NULL,
    file_id UUID NOT NULL,
    content_version BIGINT NOT NULL,
    extraction_generation BIGINT NOT NULL,
    parser_version VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('READY','EMPTY','SKIPPED','FAILED')),
    text_content TEXT,
    text_length INTEGER NOT NULL DEFAULT 0 CHECK (text_length BETWEEN 0 AND 200000),
    truncated BOOLEAN NOT NULL DEFAULT FALSE,
    error_code VARCHAR(50),
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (workspace_id, file_id, content_version, extraction_generation),
    FOREIGN KEY (workspace_id, file_id) REFERENCES files(workspace_id, id),
    CHECK (text_content IS NULL OR char_length(text_content) <= 200000)
);

CREATE TABLE task_files (
    workspace_id UUID NOT NULL,
    task_id UUID NOT NULL,
    file_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    PRIMARY KEY (workspace_id, task_id, file_id),
    FOREIGN KEY (workspace_id, task_id) REFERENCES tasks(workspace_id, id),
    FOREIGN KEY (workspace_id, file_id) REFERENCES files(workspace_id, id)
);
CREATE TABLE note_files (
    workspace_id UUID NOT NULL,
    note_id UUID NOT NULL,
    file_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    PRIMARY KEY (workspace_id, note_id, file_id),
    FOREIGN KEY (workspace_id, note_id) REFERENCES notes(workspace_id, id),
    FOREIGN KEY (workspace_id, file_id) REFERENCES files(workspace_id, id)
);
CREATE INDEX idx_task_files_file ON task_files(workspace_id, file_id);
CREATE INDEX idx_note_files_file ON note_files(workspace_id, file_id);
