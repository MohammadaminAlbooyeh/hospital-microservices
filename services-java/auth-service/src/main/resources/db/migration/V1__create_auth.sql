CREATE TABLE IF NOT EXISTS user_accounts (
    id             UUID PRIMARY KEY,
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    email          VARCHAR(320) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    first_name     VARCHAR(128) NOT NULL,
    last_name      VARCHAR(128) NOT NULL,
    phone          VARCHAR(64),
    roles          VARCHAR(255) NOT NULL DEFAULT 'PATIENT',
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    otp_code       VARCHAR(16),
    otp_expires_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_user_accounts_email ON user_accounts (email);
CREATE INDEX IF NOT EXISTS idx_user_accounts_roles ON user_accounts (roles);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         UUID PRIMARY KEY,
    version    BIGINT       NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    token      VARCHAR(512) NOT NULL UNIQUE,
    user_id    UUID         NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
        REFERENCES user_accounts (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens (token);

CREATE TABLE IF NOT EXISTS outbox (
    id             VARCHAR(64) PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id   VARCHAR(255) NOT NULL,
    event_type     VARCHAR(255) NOT NULL,
    payload        TEXT         NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    published      BOOLEAN      NOT NULL DEFAULT FALSE,
    published_at   TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_published ON outbox (published);
