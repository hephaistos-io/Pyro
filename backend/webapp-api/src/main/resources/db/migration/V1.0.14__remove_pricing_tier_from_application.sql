-- Remove pricing_tier column from application table
-- This removes the application-level pricing tier concept
-- Pricing tiers now exist only at the environment level
ALTER TABLE application
    DROP COLUMN IF EXISTS pricing_tier;
