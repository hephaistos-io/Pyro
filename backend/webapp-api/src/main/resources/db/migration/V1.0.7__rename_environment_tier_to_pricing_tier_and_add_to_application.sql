-- Rename environment_tier enum to pricing_tier and add to application table
-- The pricing_tier is now used for both environments and applications:
-- - First application for a company is FREE
-- - Subsequent applications are PAID
-- - Default environment for each application is FREE
-- - Additional environments are PAID

-- Rename the enum type from environment_tier to pricing_tier
ALTER TYPE environment_tier RENAME TO pricing_tier;

-- Add pricing_tier column to application table
-- Default to PAID for existing applications (they were created before this feature)
ALTER TABLE application
    ADD COLUMN pricing_tier pricing_tier NOT NULL DEFAULT 'PAID';
