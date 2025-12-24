-- Create usage_daily_statistics table for storing aggregated daily usage metrics
CREATE TABLE usage_daily_statistics
(
    id                       UUID PRIMARY KEY         DEFAULT uuidv7(),
    environment_id           UUID           NOT NULL REFERENCES environment (id) ON DELETE CASCADE,
    date                     DATE           NOT NULL,
    total_requests           BIGINT         NOT NULL  DEFAULT 0,
    peak_requests_per_second INTEGER        NOT NULL  DEFAULT 0,
    avg_requests_per_second  NUMERIC(10, 2) NOT NULL  DEFAULT 0,
    created_at               TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at               TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE (environment_id, date)
);

-- Index for efficient queries by environment and date range
CREATE INDEX idx_usage_daily_stats_env_date ON usage_daily_statistics (environment_id, date DESC);
