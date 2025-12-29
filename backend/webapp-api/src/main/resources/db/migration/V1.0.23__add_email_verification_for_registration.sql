-- Add email_verified column to customer table
ALTER TABLE customer
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Set existing customers as verified (they registered before this feature)
UPDATE customer
SET email_verified = TRUE;

-- Email verification tokens for new registrations
CREATE TABLE registration_verification_token
(
    id          UUID                 DEFAULT uuidv7() PRIMARY KEY,
    token       VARCHAR(64) NOT NULL UNIQUE,
    customer_id UUID        NOT NULL REFERENCES customer (id) ON DELETE CASCADE,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMP   NOT NULL,
    used_at     TIMESTAMP
);

-- Index for fast token lookup
CREATE INDEX idx_registration_verification_token ON registration_verification_token (token);

-- Index for cleanup queries (finding expired tokens)
CREATE INDEX idx_registration_verification_expires ON registration_verification_token (expires_at);

-- Grant permissions to webapp user
GRANT SELECT, INSERT, UPDATE, DELETE ON registration_verification_token TO "webapp-flagforge";
