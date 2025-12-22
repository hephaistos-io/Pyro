-- Add environment tier enum to distinguish between different pricing tiers
-- The default environments created with each application are FREE and cannot be deleted

-- Create the enum type with all tier values
CREATE TYPE environment_tier AS ENUM ('FREE', 'BASIC', 'STANDARD', 'PRO', 'BUSINESS');

-- Add the tier column with default value BASIC (custom environments)
ALTER TABLE environment
    ADD COLUMN tier environment_tier NOT NULL DEFAULT 'BASIC';