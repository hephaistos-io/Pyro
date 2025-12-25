package io.hephaistos.flagforge.customerapi.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for template caching.
 *
 * @param enabled    Whether caching is enabled (default: true)
 * @param ttlSeconds TTL for cache entries in seconds (default: 300 = 5 minutes)
 */
@ConfigurationProperties(prefix = "flagforge.cache")
public record CacheProperties(boolean enabled, int ttlSeconds) {
    public CacheProperties {
        if (ttlSeconds <= 0) {
            ttlSeconds = 300;
        }
    }
}
