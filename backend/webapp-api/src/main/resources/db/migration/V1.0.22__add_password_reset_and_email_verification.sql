-- Add password_changed_at for JWT invalidation after password change
ALTER TABLE customer
    ADD COLUMN password_changed_at TIMESTAMP;

-- Password reset tokens
CREATE TABLE password_reset_token
(
    id          UUID                 DEFAULT uuidv7() PRIMARY KEY,
    token       VARCHAR(64) NOT NULL UNIQUE,
    customer_id UUID        NOT NULL REFERENCES customer (id) ON DELETE CASCADE,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMP   NOT NULL,
    used_at     TIMESTAMP
);

-- Email change verification tokens
CREATE TABLE email_change_token
(
    id          UUID                  DEFAULT uuidv7() PRIMARY KEY,
    token       VARCHAR(64)  NOT NULL UNIQUE,
    customer_id UUID         NOT NULL REFERENCES customer (id) ON DELETE CASCADE,
    new_email   VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMP    NOT NULL,
    used_at     TIMESTAMP
);

-- Indexes for fast token lookups
CREATE INDEX idx_password_reset_token ON password_reset_token (token);
CREATE INDEX idx_email_change_token ON email_change_token (token);

-- Indexes for cleanup queries (finding expired tokens)
CREATE INDEX idx_password_reset_expires ON password_reset_token (expires_at);
CREATE INDEX idx_email_change_expires ON email_change_token (expires_at);

-- Grant permissions to webapp user
GRANT SELECT, INSERT, UPDATE, DELETE ON password_reset_token TO "webapp-flagforge";
GRANT SELECT, INSERT, UPDATE, DELETE ON email_change_token TO "webapp-flagforge";
