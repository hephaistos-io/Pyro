package io.hephaistos.flagforge;

import com.redis.testcontainers.RedisContainer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
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
    public RedisClient testRedisClient() {
        return RedisClient.create(RedisURI.create(REDIS.getRedisURI()));
    }

    @Bean
    @Primary
    public StatefulRedisConnection<String, String> testRedisConnection(
            RedisClient testRedisClient) {
        return testRedisClient.connect();
    }
}
