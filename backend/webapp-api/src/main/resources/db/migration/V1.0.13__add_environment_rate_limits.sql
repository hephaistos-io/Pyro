-- Add rate limiting fields to environment table instead of api_key table
-- This prevents abuse by rotating API keys to get more requests
-- Each environment has a rate limit per second and a monthly request quota

-- Add rate limit per second to environment (default 5 req/sec)
ALTER TABLE environment
    ADD COLUMN rate_limit_requests_per_second INTEGER NOT NULL DEFAULT 5;

-- Add monthly request quota to environment (default 500,000 requests/month)
ALTER TABLE environment
    ADD COLUMN requests_per_month INTEGER NOT NULL DEFAULT 500000;

-- Remove rate limit from api_key table (it was in the wrong place)
ALTER TABLE api_key
    DROP COLUMN IF EXISTS rate_limit_requests_per_second;

-- Remove the old per-minute rate limit if it still exists
ALTER TABLE api_key
    DROP COLUMN IF EXISTS rate_limit_requests_per_minute;
