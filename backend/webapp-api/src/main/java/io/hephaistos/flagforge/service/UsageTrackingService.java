package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.DailyUsageStatisticsResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service for retrieving usage tracking data from Redis and database.
 */
public interface UsageTrackingService {

    /**
     * Gets the current month's usage count for an environment.
     *
     * @param environmentId the environment ID
     * @return the number of API hits this month, or 0 if no data exists
     */
    long getMonthlyUsage(UUID environmentId);

    /**
     * Gets daily usage statistics for an environment for the specified number of past days.
     *
     * @param environmentId the environment ID
     * @param days          the number of days to retrieve (e.g., 7, 15, 30)
     * @return list of daily statistics, ordered by date descending (most recent first)
     */
    List<DailyUsageStatisticsResponse> getDailyStatistics(UUID environmentId, int days);
}
