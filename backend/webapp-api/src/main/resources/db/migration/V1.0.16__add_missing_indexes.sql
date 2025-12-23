-- Add missing indexes on foreign key columns for better query performance
-- PostgreSQL doesn't automatically create indexes on foreign keys

-- Index for faster environment lookups by application
CREATE INDEX IF NOT EXISTS idx_environment_application_id ON environment (application_id);

-- Index for faster template lookups by application
CREATE INDEX IF NOT EXISTS idx_template_application_id ON template (application_id);

-- Index for faster template_values lookups by identifier
CREATE INDEX IF NOT EXISTS idx_template_values_identifier ON template_values (identifier);

-- Index for faster customer_application_access lookups
CREATE INDEX IF NOT EXISTS idx_customer_application_access_customer_id
    ON customer_application_access (customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_application_access_application_id
    ON customer_application_access (application_id);
