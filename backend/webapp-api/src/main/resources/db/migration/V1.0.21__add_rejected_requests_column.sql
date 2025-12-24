-- Add rejected_requests column to track rate-limited requests per day
ALTER TABLE usage_daily_statistics
    ADD COLUMN rejected_requests BIGINT NOT NULL DEFAULT 0;
