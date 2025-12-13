-- Add environment tier enum to distinguish between FREE and PAID environments
-- The default environment created with each application is FREE and cannot be deleted

-- Create the enum type
CREATE TYPE environment_tier AS ENUM ('FREE', 'PAID');

-- Add the tier column with default value PAID (for existing environments)
ALTER TABLE environment
    ADD COLUMN tier environment_tier NOT NULL DEFAULT 'PAID';