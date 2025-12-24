package io.hephaistos.flagforge.service;

/**
 * Service for aggregating usage statistics from Redis into the database. Runs periodically to
 * consolidate daily usage data for historical queries.
 */
public interface UsageAggregationService {

    /**
     * Aggregate daily statistics from Redis into the database. Reads daily counters and peak values
     * from Redis for all environments and upserts them into the usage_daily_statistics table.
     */
    void aggregateDailyStatistics();
}
