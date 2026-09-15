CREATE TABLE projects (
    id           UUID PRIMARY KEY,
    workspace_id UUID         NOT NULL REFERENCES workspaces (id),
    name         VARCHAR(200) NOT NULL,
    description  TEXT,
    status       VARCHAR(20)  NOT NULL,
    priority     VARCHAR(20)  NOT NULL,
    start_date   DATE,
    due_date     DATE,
    archived_at  TIMESTAMPTZ,
    created_by   UUID         NOT NULL REFERENCES users (id),
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    deleted_at   TIMESTAMPTZ,
    version      BIGINT       NOT NULL,
    CONSTRAINT projects_status_chk CHECK (status IN ('PLANNED', 'ACTIVE', 'PAUSED', 'COMPLETED')),
    CONSTRAINT projects_priority_chk CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT projects_dates_chk CHECK (due_date IS NULL OR start_date IS NULL OR due_date >= start_date)
);

CREATE INDEX idx_projects_workspace_status
    ON projects (workspace_id, status)
    WHERE deleted_at IS NULL AND archived_at IS NULL;

CREATE INDEX idx_projects_workspace_archived
    ON projects (workspace_id, archived_at)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_projects_workspace_updated_at
    ON projects (workspace_id, updated_at DESC)
    WHERE deleted_at IS NULL;
