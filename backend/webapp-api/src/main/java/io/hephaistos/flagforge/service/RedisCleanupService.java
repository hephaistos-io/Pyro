package io.hephaistos.flagforge.service;

import java.util.UUID;

/**
 * Service for cleaning up Redis keys when resources are deleted. Ensures orphaned rate limit
 * buckets and usage counters are removed.
 */
public interface RedisCleanupService {

    /**
     * Clean up all Redis keys associated with an environment. This includes rate limit buckets,
     * usage counters, and cached templates.
     *
     * @param environmentId The ID of the deleted environment
     */
    void cleanupEnvironmentKeys(UUID environmentId);
}
