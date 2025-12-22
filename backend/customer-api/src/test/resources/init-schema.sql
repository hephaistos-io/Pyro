-- Test database schema initialization for customer-api
-- This script sets up the necessary tables for integration testing

-- Create company table
CREATE TABLE company
(
    id   UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

-- Create application table
CREATE TABLE application
(
    id         UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    company_id UUID         NOT NULL,
    CONSTRAINT fk_application_company FOREIGN KEY (company_id) REFERENCES company (id)
);

CREATE UNIQUE INDEX idx_application_name_company ON application (name, company_id);
CREATE INDEX idx_application_company_id ON application (company_id);

-- Create environment table
CREATE TABLE environment
(
    id                             UUID                  DEFAULT gen_random_uuid() PRIMARY KEY,
    name                           VARCHAR(255) NOT NULL,
    application_id                 UUID         NOT NULL,
    rate_limit_requests_per_second INTEGER      NOT NULL DEFAULT 5,
    requests_per_month             INTEGER      NOT NULL DEFAULT 500000,
    CONSTRAINT fk_environment_application FOREIGN KEY (application_id)
        REFERENCES application (id) ON DELETE CASCADE
);

-- Create api_key table
CREATE TABLE api_key
(
    id                             UUID                     DEFAULT gen_random_uuid() PRIMARY KEY,
    key_hash                       VARCHAR(64)  NOT NULL,
    key_prefix                     VARCHAR(8)   NOT NULL,
    application_id                 UUID         NOT NULL,
    environment_id UUID NOT NULL,
    name                           VARCHAR(255) NOT NULL,
    created_at                     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_used_at                   TIMESTAMP WITH TIME ZONE,
    expires_at                     TIMESTAMP WITH TIME ZONE,
    is_active                      BOOLEAN                  DEFAULT TRUE,
    CONSTRAINT fk_api_key_application FOREIGN KEY (application_id)
        REFERENCES application (id) ON DELETE CASCADE,
    CONSTRAINT fk_api_key_environment FOREIGN KEY (environment_id)
        REFERENCES environment (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_api_key_hash ON api_key (key_hash);
CREATE INDEX idx_api_key_application_id ON api_key (application_id);
CREATE INDEX idx_api_key_active ON api_key (is_active) WHERE is_active = TRUE;
