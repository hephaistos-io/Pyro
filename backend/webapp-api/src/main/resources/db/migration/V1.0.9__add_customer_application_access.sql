-- Create join table for customer-application access
-- This table tracks which customers have access to which applications
-- When a customer creates an application, they automatically get access to it

CREATE TABLE customer_application_access
(
    id UUID DEFAULT uuidv7() PRIMARY KEY,
    customer_id    UUID                     NOT NULL,
    application_id UUID                     NOT NULL,
    CONSTRAINT fk_customer FOREIGN KEY (customer_id) REFERENCES customer (id) ON DELETE CASCADE,
    CONSTRAINT fk_application FOREIGN KEY (application_id) REFERENCES application (id) ON DELETE CASCADE,
    CONSTRAINT uq_customer_application UNIQUE (customer_id, application_id)
);

-- Index for efficient lookups by customer
CREATE INDEX idx_customer_application_access_customer ON customer_application_access (customer_id);

-- Index for efficient lookups by application
CREATE INDEX idx_customer_application_access_application ON customer_application_access (application_id);
