package io.hephaistos.flagforge.customerapi.configuration;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis configuration for rate limiting using Lettuce client. Only active when rate limiting is
 * enabled.
 */
@Configuration
@ConditionalOnProperty(name = "flagforge.rate-limit.enabled", havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(RateLimitProperties.class)
public class RedisConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConfiguration.class);

    @Bean(destroyMethod = "shutdown")
    public RedisClient redisClient(RateLimitProperties properties) {
        LOGGER.info("Creating Redis client with URI: {}", properties.redisUri());
        return RedisClient.create(RedisURI.create(properties.redisUri()));
    }

    /**
     * Connection for bucket4j rate limiting (requires byte[] values).
     */
    @Bean(name = "rateLimitRedisConnection", destroyMethod = "close")
    public StatefulRedisConnection<String, byte[]> rateLimitRedisConnection(
            RedisClient redisClient) {
        LOGGER.info("Establishing Redis connection for rate limiting (byte[] codec)");
        return redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

    /**
     * Connection for usage tracking (uses String values).
     */
    @Bean(name = "usageRedisConnection", destroyMethod = "close")
    public StatefulRedisConnection<String, String> usageRedisConnection(RedisClient redisClient) {
        LOGGER.info("Establishing Redis connection for usage tracking (String codec)");
        return redisClient.connect();
    }
}
