package io.hephaistos.flagforge.customerapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * No-op implementation of RateLimitService when rate limiting is disabled. All requests are allowed
 * and usage is not tracked.
 */
@Service
@ConditionalOnProperty(name = "flagforge.rate-limit.enabled", havingValue = "false")
public class NoOpRateLimitService implements RateLimitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpRateLimitService.class);

    public NoOpRateLimitService() {
        LOGGER.info("Rate limiting is disabled - using no-op implementation");
    }

    @Override
    public RateLimitResult tryConsume(UUID environmentId, int requestsPerSecond) {
        return RateLimitResult.allowed(requestsPerSecond);
    }

    @Override
    public long incrementMonthlyUsage(UUID environmentId) {
        return 0;
    }

    @Override
    public long getMonthlyUsage(UUID environmentId) {
        return 0;
    }

    @Override
    public long getRemainingMonthlyQuota(UUID environmentId, long monthlyLimit) {
        return monthlyLimit;
    }

    @Override
    public void incrementDailyUsage(UUID environmentId) {
        // No-op
    }

    @Override
    public void trackPeakBurst(UUID environmentId) {
        // No-op
    }

    @Override
    public void incrementRejectedRequests(UUID environmentId) {
        // No-op
    }
}
