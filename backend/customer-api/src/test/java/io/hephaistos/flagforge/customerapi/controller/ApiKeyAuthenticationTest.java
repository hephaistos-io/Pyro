package io.hephaistos.flagforge.customerapi.controller;

import io.hephaistos.flagforge.customerapi.IntegrationTestSupport;
import io.hephaistos.flagforge.customerapi.PostgresTestcontainerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestcontainerConfiguration.class)
@Tag("integration")
class ApiKeyAuthenticationTest extends IntegrationTestSupport {

    @BeforeEach
    void setUp() {
        initializeTestSupport();
    }

    @Test
    void validApiKey_shouldReturnSuccess() {
        var response = get("/v1/flags", List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void invalidApiKey_shouldReturnUnauthorized() {
        var response = get("/v1/flags", "invalid_key", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void missingApiKey_shouldReturnUnauthorized() {
        var response = getUnauthenticated("/v1/flags", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void inactiveApiKey_shouldReturnUnauthorized() {
        String inactiveKey = "ff_test_inactive_key_1234567890123456";
        createApiKey(inactiveKey, testApplicationId, false, 1000);

        var response = get("/v1/flags", inactiveKey, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void expiredApiKey_shouldReturnUnauthorized() {
        String expiredKey = "ff_test_expired_key_12345678901234567";
        createExpiredApiKey(expiredKey, testApplicationId);

        var response = get("/v1/flags", expiredKey, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void openApiDocs_shouldBeAccessibleWithoutAuth() {
        var response = getUnauthenticated("/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
