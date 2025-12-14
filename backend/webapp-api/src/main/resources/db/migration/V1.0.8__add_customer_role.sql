-- Add customer role enum for access control
-- Roles are hierarchical: ADMIN > DEV > READ_ONLY
-- - ADMIN: Full access to all features
-- - DEV: Read/write access to certain elements
-- - READ_ONLY: Only read access

-- Create the enum type
CREATE TYPE customer_role AS ENUM ('READ_ONLY', 'DEV', 'ADMIN');

-- Add the role column with default value READ_ONLY (for existing customers)
-- New customers registered via the application will have ADMIN set in application code
ALTER TABLE customer
    ADD COLUMN role customer_role NOT NULL DEFAULT 'READ_ONLY';
