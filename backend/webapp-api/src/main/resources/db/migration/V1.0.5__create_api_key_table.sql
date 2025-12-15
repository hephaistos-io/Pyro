-- Create api_key table for customer-api authentication
-- API keys are associated with applications and environments for SDK integration

-- Create the key_type enum
-- READ: Key can only read data (feature flags, configurations)
-- WRITE: Key can read and write data
CREATE TYPE key_type AS ENUM ('READ', 'WRITE');

CREATE TABLE api_key
(
    id                             UUID DEFAULT uuidv7() PRIMARY KEY,
    key_hash                       VARCHAR(64) NOT NULL,
    application_id                 UUID        NOT NULL,
    environment_id                 UUID        NOT NULL,
    rate_limit_requests_per_minute INTEGER     NOT NULL,
    key_type                       key_type    NOT NULL,
    CONSTRAINT fk_api_key_application FOREIGN KEY (application_id)
        REFERENCES application (id) ON DELETE CASCADE,
    CONSTRAINT fk_api_key_environment FOREIGN KEY (environment_id)
        REFERENCES environment (id) ON DELETE CASCADE
);

-- Index for fast key lookup during authentication (hash-based)
CREATE UNIQUE INDEX idx_api_key_hash ON api_key (key_hash);

-- Index for listing keys by application
CREATE INDEX idx_api_key_application_id ON api_key (application_id);

-- Index for listing keys by environment
CREATE INDEX idx_api_key_environment_id ON api_key (environment_id);
