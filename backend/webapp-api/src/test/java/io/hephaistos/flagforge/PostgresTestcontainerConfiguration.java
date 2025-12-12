package io.hephaistos.flagforge;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Test configuration that provides a PostgreSQL Testcontainer for integration tests.
 * <p>
 * Uses {@link ServiceConnection} for automatic Spring Boot datasource configuration. The container
 * is shared across all tests for performance.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainerConfiguration {

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("flagforge")
                    .withUsername("flagforge")
                    .withPassword("flagforge");

    static {
        POSTGRES.start();
    }

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return POSTGRES;
    }
}
