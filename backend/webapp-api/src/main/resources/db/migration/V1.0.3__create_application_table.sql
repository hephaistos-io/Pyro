-- Create application table
-- Applications belong to a company (many-to-one relationship)

CREATE TABLE application
(
    id         UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    company_id UUID         NOT NULL,
    CONSTRAINT fk_application_company FOREIGN KEY (company_id)
        REFERENCES company (id)
);

-- Unique constraint: application name must be unique within a company
CREATE UNIQUE INDEX idx_application_name_company ON application (name, company_id);

-- Index for faster lookups by company
CREATE INDEX idx_application_company_id ON application (company_id);
