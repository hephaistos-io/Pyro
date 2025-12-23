package io.hephaistos.flagforge.customerapi.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for rate limiting.
 *
 * @param enabled  Whether rate limiting is enabled (default: true)
 * @param redisUri Redis connection URI (default: redis://localhost:6379)
 * @param failOpen If true, allow requests when Redis is unavailable (default: true)
 */
@ConfigurationProperties(prefix = "flagforge.rate-limit")
public record RateLimitProperties(boolean enabled, String redisUri, boolean failOpen) {
    public RateLimitProperties {
        if (redisUri == null || redisUri.isBlank()) {
            redisUri = "redis://localhost:6379";
        }
    }
}
