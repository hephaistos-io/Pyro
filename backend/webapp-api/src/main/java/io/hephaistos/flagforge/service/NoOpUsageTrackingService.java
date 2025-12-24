package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.DailyUsageStatisticsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * No-op implementation of UsageTrackingService. Used when Redis is disabled (e.g., during OpenAPI
 * generation).
 */
@Service
@ConditionalOnProperty(name = "flagforge.redis.enabled", havingValue = "false")
public class NoOpUsageTrackingService implements UsageTrackingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpUsageTrackingService.class);

    public NoOpUsageTrackingService() {
        LOGGER.info("Using NoOp usage tracking service (Redis disabled)");
    }

    @Override
    public long getMonthlyUsage(UUID environmentId) {
        return 0L;
    }

    @Override
    public List<DailyUsageStatisticsResponse> getDailyStatistics(UUID environmentId, int days) {
        return Collections.emptyList();
    }
}
