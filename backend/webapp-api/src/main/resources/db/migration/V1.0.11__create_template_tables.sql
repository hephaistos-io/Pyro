-- Template feature: configuration templates with JSONB schema and override values
-- - USER templates: user attributes with editable flags for settings UI
-- - SYSTEM templates: static configurations per identifier

-- Create the template type enum
CREATE TYPE template_type AS ENUM ('USER', 'SYSTEM');

-- One template per (application, type) - schema contains field definitions with defaults
CREATE TABLE template
(
    id             UUID                   DEFAULT uuidv7() PRIMARY KEY,
    application_id UUID          NOT NULL REFERENCES application (id) ON DELETE CASCADE,
    company_id     UUID          NOT NULL REFERENCES company (id) ON DELETE CASCADE,
    type           template_type NOT NULL,
    schema         JSONB         NOT NULL DEFAULT '{}',
    created_at     TIMESTAMP              DEFAULT NOW(),
    updated_at     TIMESTAMP              DEFAULT NOW(),
    CONSTRAINT uq_template_app_type UNIQUE (application_id, type)
);

-- Override values per (application, environment, type, identifier) - no defaults row
CREATE TABLE template_values
(
    id             UUID                   DEFAULT uuidv7() PRIMARY KEY,
    application_id UUID          NOT NULL REFERENCES application (id) ON DELETE CASCADE,
    environment_id UUID          NOT NULL REFERENCES environment (id) ON DELETE CASCADE,
    type           template_type NOT NULL,
    identifier     VARCHAR(255)  NOT NULL,
    values         JSONB         NOT NULL DEFAULT '{}',
    created_at     TIMESTAMP              DEFAULT NOW(),
    updated_at     TIMESTAMP              DEFAULT NOW(),
    CONSTRAINT uq_template_values UNIQUE (application_id, environment_id, type, identifier)
);

-- Indexes for performance
CREATE INDEX idx_template_application ON template (application_id);
CREATE INDEX idx_template_company ON template (company_id);
CREATE INDEX idx_template_values_lookup ON template_values (application_id, environment_id, type);
