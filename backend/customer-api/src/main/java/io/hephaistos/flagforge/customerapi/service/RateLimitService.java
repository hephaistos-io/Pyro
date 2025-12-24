package io.hephaistos.flagforge.customerapi.service;

import java.util.UUID;

/**
 * Service for distributed rate limiting and usage tracking. Uses Redis for state management across
 * multiple customer-api instances.
 */
public interface RateLimitService {

    /**
     * Attempt to consume a token for the given environment.
     *
     * @param environmentId     The environment UUID
     * @param requestsPerSecond The rate limit for this environment
     * @return RateLimitResult indicating success/failure and remaining tokens
     */
    RateLimitResult tryConsume(UUID environmentId, int requestsPerSecond);

    /**
     * Increment the monthly usage counter for the environment.
     *
     * @param environmentId The environment UUID
     * @return The new counter value
     */
    long incrementMonthlyUsage(UUID environmentId);

    /**
     * Get current monthly usage for the environment.
     *
     * @param environmentId The environment UUID
     * @return Current usage count for the month
     */
    long getMonthlyUsage(UUID environmentId);

    /**
     * Get remaining quota for the month.
     *
     * @param environmentId The environment UUID
     * @param monthlyLimit  The monthly request limit
     * @return Remaining requests available
     */
    long getRemainingMonthlyQuota(UUID environmentId, long monthlyLimit);

    /**
     * Increment the daily usage counter for the environment.
     *
     * @param environmentId The environment UUID
     */
    void incrementDailyUsage(UUID environmentId);

    /**
     * Track peak requests per second for the environment. Updates the daily peak if the current
     * second's request count exceeds the stored peak.
     *
     * @param environmentId The environment UUID
     */
    void trackPeakBurst(UUID environmentId);

    /**
     * Increment the daily rejected requests counter for the environment. Called when a request is
     * denied due to rate limiting.
     *
     * @param environmentId The environment UUID
     */
    void incrementRejectedRequests(UUID environmentId);

    /**
     * Result of a rate limit check.
     *
     * @param allowed          Whether the request is allowed
     * @param remainingTokens  Number of tokens remaining in the bucket
     * @param retryAfterMillis Milliseconds to wait before retrying (if denied)
     */
    record RateLimitResult(boolean allowed, long remainingTokens, long retryAfterMillis) {
        public static RateLimitResult allowed(long remaining) {
            return new RateLimitResult(true, remaining, 0);
        }

        public static RateLimitResult denied(long retryAfterMillis) {
            return new RateLimitResult(false, 0, retryAfterMillis);
        }
    }
}
