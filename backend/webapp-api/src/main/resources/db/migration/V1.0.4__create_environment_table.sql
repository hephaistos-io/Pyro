-- Create environment table for managing deployment environments per application
-- Environments belong to an application (many-to-one relationship)

CREATE TABLE environment
(
    id             UUID DEFAULT uuidv7() PRIMARY KEY,
    application_id UUID         NOT NULL,
    name           VARCHAR(255) NOT NULL,
    description    VARCHAR(255),
    CONSTRAINT fk_environment_application FOREIGN KEY (application_id)
        REFERENCES application (id) ON DELETE CASCADE
);

-- Unique constraint: environment name must be unique within an application
CREATE UNIQUE INDEX idx_environment_name_application ON environment (name, application_id);

-- Index for faster lookups by application
CREATE INDEX idx_environment_application_id ON environment (application_id);
