CREATE TABLE tasks (
    id            UUID PRIMARY KEY,
    workspace_id  UUID         NOT NULL REFERENCES workspaces (id),
    project_id    UUID         REFERENCES projects (id),
    parent_id     UUID         REFERENCES tasks (id),
    title         VARCHAR(300) NOT NULL,
    description   TEXT,
    status        VARCHAR(20)  NOT NULL,
    priority      VARCHAR(20)  NOT NULL,
    start_at      TIMESTAMPTZ,
    due_at        TIMESTAMPTZ,
    completed_at  TIMESTAMPTZ,
    created_by    UUID         NOT NULL REFERENCES users (id),
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    deleted_at    TIMESTAMPTZ,
    version       BIGINT       NOT NULL,
    CONSTRAINT tasks_status_chk CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED')),
    CONSTRAINT tasks_priority_chk CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT tasks_times_chk CHECK (due_at IS NULL OR start_at IS NULL OR due_at >= start_at)
);

CREATE INDEX idx_tasks_workspace_status
    ON tasks (workspace_id, status)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_tasks_workspace_due_at
    ON tasks (workspace_id, due_at)
    WHERE deleted_at IS NULL AND status NOT IN ('DONE', 'CANCELLED');

CREATE INDEX idx_tasks_project_id
    ON tasks (project_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_tasks_parent_id
    ON tasks (parent_id)
    WHERE deleted_at IS NULL;
