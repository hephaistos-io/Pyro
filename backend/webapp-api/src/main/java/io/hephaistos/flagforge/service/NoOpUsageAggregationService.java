package io.hephaistos.flagforge.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * No-op implementation of UsageAggregationService used when Redis is disabled. The scheduled
 * aggregation task is a no-op since there's no Redis data to aggregate.
 */
@Service
@ConditionalOnProperty(name = "flagforge.redis.enabled", havingValue = "false",
        matchIfMissing = true)
public class NoOpUsageAggregationService implements UsageAggregationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpUsageAggregationService.class);

    public NoOpUsageAggregationService() {
        LOGGER.info("Using NoOp usage aggregation service (Redis disabled)");
    }

    @Override
    public void aggregateDailyStatistics() {
        LOGGER.info("Redis not enabled, skipping daily aggregation");
    }
}
