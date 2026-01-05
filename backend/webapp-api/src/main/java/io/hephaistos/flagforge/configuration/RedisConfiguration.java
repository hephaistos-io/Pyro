package io.hephaistos.flagforge.configuration;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis configuration for reading usage tracking data. Only active when flagforge.redis.enabled is
 * true (default).
 */
@Configuration
@ConditionalOnProperty(name = "flagforge.redis.enabled", havingValue = "true")
public class RedisConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConfiguration.class);

    @Value("${flagforge.redis.uri:redis://localhost:6379}")
    private String redisUri;

    @Bean(destroyMethod = "shutdown")
    public RedisClient redisClient() {
        LOGGER.info("Creating Redis client with URI: {}", redisUri);
        return RedisClient.create(RedisURI.create(redisUri));
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, String> redisConnection(RedisClient redisClient) {
        LOGGER.info("Establishing Redis connection for usage tracking");
        return redisClient.connect();
    }
}
