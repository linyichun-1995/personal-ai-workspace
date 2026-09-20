ALTER TABLE projects ADD COLUMN tag_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE tasks ADD COLUMN tag_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE notes ADD COLUMN tag_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE tags (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    name VARCHAR(100) NOT NULL,
    normalized_name VARCHAR(100) NOT NULL,
    color VARCHAR(20) NOT NULL CHECK (color IN ('GRAY','RED','ORANGE','YELLOW','GREEN','BLUE','PURPLE','PINK')),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (workspace_id, id),
    UNIQUE (workspace_id, normalized_name)
);
CREATE TABLE project_tags (
    workspace_id UUID NOT NULL,
    source_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (workspace_id, source_id, tag_id),
    FOREIGN KEY (workspace_id, source_id) REFERENCES projects(workspace_id,id),
    FOREIGN KEY (workspace_id, tag_id) REFERENCES tags(workspace_id,id) ON DELETE CASCADE
);
CREATE TABLE task_tags (
    workspace_id UUID NOT NULL,
    source_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (workspace_id, source_id, tag_id),
    FOREIGN KEY (workspace_id, source_id) REFERENCES tasks(workspace_id,id),
    FOREIGN KEY (workspace_id, tag_id) REFERENCES tags(workspace_id,id) ON DELETE CASCADE
);
CREATE TABLE note_tags (
    workspace_id UUID NOT NULL,
    source_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (workspace_id, source_id, tag_id),
    FOREIGN KEY (workspace_id, source_id) REFERENCES notes(workspace_id,id),
    FOREIGN KEY (workspace_id, tag_id) REFERENCES tags(workspace_id,id) ON DELETE CASCADE
);
CREATE TABLE file_tags (
    workspace_id UUID NOT NULL,
    source_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (workspace_id, source_id, tag_id),
    FOREIGN KEY (workspace_id, source_id) REFERENCES files(workspace_id,id),
    FOREIGN KEY (workspace_id, tag_id) REFERENCES tags(workspace_id,id) ON DELETE CASCADE
);
CREATE INDEX idx_project_tags_tag ON project_tags(workspace_id,tag_id);
CREATE INDEX idx_task_tags_tag ON task_tags(workspace_id,tag_id);
CREATE INDEX idx_note_tags_tag ON note_tags(workspace_id,tag_id);
CREATE INDEX idx_file_tags_tag ON file_tags(workspace_id,tag_id);

CREATE TABLE background_jobs (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    job_type VARCHAR(30) NOT NULL CHECK (job_type IN ('EXTRACT_FILE','UPSERT_SEARCH','REMOVE_SEARCH','PURGE_FILE')),
    source_type VARCHAR(10) NOT NULL CHECK (source_type IN ('PROJECT','TASK','NOTE','FILE')),
    source_id UUID NOT NULL,
    source_version BIGINT NOT NULL,
    generation BIGINT NOT NULL DEFAULT 1,
    dedupe_key VARCHAR(250) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','RUNNING','SUCCEEDED','DEAD')),
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    next_run_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lease_token UUID,
    lease_until TIMESTAMPTZ,
    error_code VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_jobs_ready ON background_jobs(status,next_run_at);
CREATE INDEX idx_jobs_leases ON background_jobs(status,lease_until);

CREATE TABLE search_documents (
    index_generation BIGINT NOT NULL,
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    source_type VARCHAR(10) NOT NULL CHECK (source_type IN ('PROJECT','TASK','NOTE','FILE')),
    source_id UUID NOT NULL,
    source_version BIGINT NOT NULL,
    extraction_generation BIGINT NOT NULL DEFAULT 0,
    title TEXT NOT NULL,
    body_text TEXT NOT NULL,
    normalized_title TEXT NOT NULL,
    normalized_body TEXT NOT NULL,
    search_bigrams TEXT ARRAY NOT NULL,
    source_updated_at TIMESTAMPTZ NOT NULL,
    indexed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (index_generation,workspace_id,source_type,source_id)
);
CREATE INDEX idx_search_scope ON search_documents(index_generation,workspace_id,source_type,source_updated_at DESC);
-- [jooq ignore start]
CREATE INDEX idx_search_bigrams ON search_documents USING GIN(search_bigrams);
-- [jooq ignore stop]
