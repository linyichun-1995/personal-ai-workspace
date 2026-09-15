CREATE TABLE users (
    id               UUID PRIMARY KEY,
    email            VARCHAR(320) NOT NULL,
    email_normalized VARCHAR(320) NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,
    display_name     VARCHAR(100) NOT NULL,
    avatar_url       VARCHAR(1000),
    locale           VARCHAR(20)  NOT NULL,
    timezone         VARCHAR(50)  NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    version          BIGINT       NOT NULL,
    CONSTRAINT users_email_normalized_uk UNIQUE (email_normalized),
    CONSTRAINT users_status_chk CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE workspaces (
    id             UUID PRIMARY KEY,
    name           VARCHAR(120) NOT NULL,
    slug           VARCHAR(80)  NOT NULL,
    type           VARCHAR(20)  NOT NULL,
    timezone       VARCHAR(50)  NOT NULL,
    week_starts_on SMALLINT     NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    version        BIGINT       NOT NULL,
    CONSTRAINT workspaces_slug_uk UNIQUE (slug),
    CONSTRAINT workspaces_type_chk CHECK (type IN ('PERSONAL')),
    CONSTRAINT workspaces_week_starts_on_chk CHECK (week_starts_on BETWEEN 1 AND 7)
);

CREATE TABLE workspace_members (
    id           UUID PRIMARY KEY,
    workspace_id UUID        NOT NULL REFERENCES workspaces (id),
    user_id      UUID        NOT NULL REFERENCES users (id),
    role         VARCHAR(20) NOT NULL,
    joined_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT workspace_members_workspace_user_uk UNIQUE (workspace_id, user_id),
    CONSTRAINT workspace_members_role_chk CHECK (role IN ('OWNER', 'MEMBER'))
);

CREATE INDEX workspace_members_user_id_idx ON workspace_members (user_id);
