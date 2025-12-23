package io.hephaistos.flagforge.customerapi;

import com.redis.testcontainers.RedisContainer;
import io.hephaistos.flagforge.customerapi.configuration.RateLimitProperties;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration that provides a Redis Testcontainer for integration tests.
 * <p>
 * The container is shared across all tests for performance.
 */
@TestConfiguration(proxyBeanMethods = false)
public class RedisTestContainerConfiguration {

    private static final RedisContainer REDIS = new RedisContainer("redis:7-alpine");

    static {
        REDIS.start();
    }

    @Bean
    public RedisContainer redisContainer() {
        return REDIS;
    }

    @Bean
    @Primary
    public RateLimitProperties rateLimitProperties() {
        return new RateLimitProperties(true, REDIS.getRedisURI(), true);
    }

    @Bean
    @Primary
    public RedisClient testRedisClient() {
        return RedisClient.create(RedisURI.create(REDIS.getRedisURI()));
    }

    @Bean(name = "rateLimitRedisConnection")
    @Primary
    public StatefulRedisConnection<String, byte[]> testRateLimitRedisConnection(
            RedisClient testRedisClient) {
        return testRedisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

    @Bean(name = "usageRedisConnection")
    @Primary
    public StatefulRedisConnection<String, String> testUsageRedisConnection(
            RedisClient testRedisClient) {
        return testRedisClient.connect();
    }
}
