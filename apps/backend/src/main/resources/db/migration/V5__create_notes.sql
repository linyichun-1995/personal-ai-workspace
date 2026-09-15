CREATE TABLE notes (
    id           UUID PRIMARY KEY,
    workspace_id UUID         NOT NULL REFERENCES workspaces (id),
    project_id   UUID         REFERENCES projects (id),
    title        VARCHAR(300) NOT NULL,
    content      TEXT         NOT NULL,
    summary      VARCHAR(500) NOT NULL,
    favorite     BOOLEAN      NOT NULL,
    archived     BOOLEAN      NOT NULL,
    created_by   UUID         NOT NULL REFERENCES users (id),
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    deleted_at   TIMESTAMPTZ,
    version      BIGINT       NOT NULL
);

CREATE INDEX idx_notes_workspace_updated_at
    ON notes (workspace_id, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_notes_project_id
    ON notes (project_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_notes_workspace_favorite
    ON notes (workspace_id, favorite)
    WHERE deleted_at IS NULL AND archived = FALSE;
