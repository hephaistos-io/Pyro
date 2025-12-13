-- Create api_key table for customer-api authentication
-- API keys are associated with applications for SDK integration

CREATE TABLE api_key
(
    id                             UUID                     DEFAULT gen_random_uuid() PRIMARY KEY,
    key_hash                       VARCHAR(64)  NOT NULL,
    key_prefix                     VARCHAR(8)   NOT NULL,
    application_id                 UUID         NOT NULL,
    name                           VARCHAR(255) NOT NULL,
    created_at                     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_used_at                   TIMESTAMP WITH TIME ZONE,
    expires_at                     TIMESTAMP WITH TIME ZONE,
    is_active                      BOOLEAN                  DEFAULT TRUE,
    rate_limit_requests_per_minute INTEGER                  DEFAULT 1000,
    CONSTRAINT fk_api_key_application FOREIGN KEY (application_id)
        REFERENCES application (id) ON DELETE CASCADE
);

-- Index for fast key lookup during authentication (hash-based)
CREATE UNIQUE INDEX idx_api_key_hash ON api_key (key_hash);

-- Index for listing keys by application
CREATE INDEX idx_api_key_application_id ON api_key (application_id);

-- Index for finding active keys efficiently
CREATE INDEX idx_api_key_active ON api_key (is_active) WHERE is_active = TRUE;
