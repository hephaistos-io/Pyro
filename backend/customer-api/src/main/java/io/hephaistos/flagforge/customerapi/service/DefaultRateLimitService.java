package io.hephaistos.flagforge.customerapi.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.hephaistos.flagforge.customerapi.configuration.RateLimitProperties;
import io.lettuce.core.api.StatefulRedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Redis-based rate limiting and usage tracking service. Uses bucket4j-redis for per-second rate
 * limiting and Redis INCR for monthly counters.
 */
@Service
@ConditionalOnProperty(name = "flagforge.rate-limit.enabled", havingValue = "true",
        matchIfMissing = true)
public class DefaultRateLimitService implements RateLimitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRateLimitService.class);
    private static final String RATE_LIMIT_KEY_PREFIX = "rate-limit:env:";
    private static final String USAGE_KEY_PREFIX = "usage:monthly:";
    private static final long USAGE_KEY_TTL_SECONDS = Duration.ofDays(45).toSeconds();

    private final ProxyManager<String> proxyManager;
    private final StatefulRedisConnection<String, String> usageRedisConnection;
    private final RateLimitProperties properties;
    private final ConcurrentHashMap<Integer, Supplier<BucketConfiguration>> configCache =
            new ConcurrentHashMap<>();

    public DefaultRateLimitService(@Qualifier("rateLimitRedisConnection")
    StatefulRedisConnection<String, byte[]> rateLimitConnection, @Qualifier("usageRedisConnection")
    StatefulRedisConnection<String, String> usageRedisConnection, RateLimitProperties properties) {
        this.usageRedisConnection = usageRedisConnection;
        this.properties = properties;
        this.proxyManager = LettuceBasedProxyManager.builderFor(rateLimitConnection).build();
        LOGGER.info("Initialized Redis-based rate limiting service");
    }

    @Override
    public RateLimitResult tryConsume(UUID environmentId, int requestsPerSecond) {
        try {
            String key = RATE_LIMIT_KEY_PREFIX + environmentId;
            var bucket = proxyManager.builder().build(key, configSupplier(requestsPerSecond));

            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (probe.isConsumed()) {
                return RateLimitResult.allowed(probe.getRemainingTokens());
            }
            else {
                long retryAfterMillis = probe.getNanosToWaitForRefill() / 1_000_000;
                return RateLimitResult.denied(retryAfterMillis);
            }
        }
        catch (Exception e) {
            LOGGER.error("Rate limit check failed for environment {}", environmentId, e);
            if (properties.failOpen()) {
                LOGGER.warn("Fail-open enabled: allowing request despite Redis error");
                return RateLimitResult.allowed(requestsPerSecond);
            }
            throw new RuntimeException("Rate limiting service unavailable", e);
        }
    }

    @Override
    public long incrementMonthlyUsage(UUID environmentId) {
        String key = getMonthlyUsageKey(environmentId);
        try {
            var commands = usageRedisConnection.sync();
            long newValue = commands.incr(key);
            // Set TTL only on first increment (when value is 1)
            if (newValue == 1) {
                commands.expire(key, USAGE_KEY_TTL_SECONDS);
            }
            return newValue;
        }
        catch (Exception e) {
            LOGGER.error("Failed to increment monthly usage for environment {}", environmentId, e);
            if (properties.failOpen()) {
                LOGGER.warn("Fail-open enabled: returning 0 for usage increment");
                return 0;
            }
            throw new RuntimeException("Usage tracking service unavailable", e);
        }
    }

    @Override
    public long getMonthlyUsage(UUID environmentId) {
        String key = getMonthlyUsageKey(environmentId);
        try {
            String value = usageRedisConnection.sync().get(key);
            return value != null ? Long.parseLong(value) : 0L;
        }
        catch (Exception e) {
            LOGGER.error("Failed to get monthly usage for environment {}", environmentId, e);
            if (properties.failOpen()) {
                return 0L;
            }
            throw new RuntimeException("Usage tracking service unavailable", e);
        }
    }

    @Override
    public long getRemainingMonthlyQuota(UUID environmentId, long monthlyLimit) {
        long usage = getMonthlyUsage(environmentId);
        return Math.max(0, monthlyLimit - usage);
    }

    private String getMonthlyUsageKey(UUID environmentId) {
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        return USAGE_KEY_PREFIX + environmentId + ":" + currentMonth;
    }

    private Supplier<BucketConfiguration> configSupplier(int requestsPerSecond) {
        return configCache.computeIfAbsent(requestsPerSecond,
                rps -> () -> BucketConfiguration.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(rps)
                                .refillGreedy(rps, Duration.ofSeconds(1))
                                .initialTokens(rps)
                                .build())
                        .build());
    }
}
