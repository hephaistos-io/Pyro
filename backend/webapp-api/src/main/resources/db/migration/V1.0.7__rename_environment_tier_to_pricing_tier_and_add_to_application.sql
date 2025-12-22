-- Rename environment_tier enum to pricing_tier and add to application table
-- The pricing_tier is now used for both environments and applications:
-- - First application for a company is FREE
-- - Subsequent applications are BASIC
-- - Default environments for each application are FREE
-- - Additional custom environments are BASIC

-- Rename the enum type from environment_tier to pricing_tier
ALTER TYPE environment_tier RENAME TO pricing_tier;

-- Add pricing_tier column to application table
-- Default to BASIC for custom applications
ALTER TABLE application
    ADD COLUMN pricing_tier pricing_tier NOT NULL DEFAULT 'BASIC';
