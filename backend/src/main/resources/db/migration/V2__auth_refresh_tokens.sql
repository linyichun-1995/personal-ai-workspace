ALTER TABLE users
    ADD COLUMN last_login_at TIMESTAMPTZ;

ALTER TABLE users DROP CONSTRAINT users_status_chk;

ALTER TABLE users
    ADD CONSTRAINT users_status_chk CHECK (status IN ('ACTIVE', 'DISABLED', 'LOCKED'));

CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id),
    token_hash VARCHAR(64) NOT NULL,
    family_id  UUID        NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT refresh_tokens_token_hash_uk UNIQUE (token_hash)
);

CREATE INDEX refresh_tokens_user_id_idx ON refresh_tokens (user_id);
CREATE INDEX refresh_tokens_family_id_idx ON refresh_tokens (family_id);
