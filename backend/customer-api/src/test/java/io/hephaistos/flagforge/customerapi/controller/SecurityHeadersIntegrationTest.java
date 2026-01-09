package io.hephaistos.flagforge.customerapi.controller;

import io.hephaistos.flagforge.customerapi.IntegrationTestSupport;
import io.hephaistos.flagforge.customerapi.PostgresTestContainerConfiguration;
import io.hephaistos.flagforge.customerapi.RedisTestContainerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests verifying that security headers are present in API responses.
 * These headers protect against common web vulnerabilities like clickjacking
 * and MIME type sniffing.
 * <p>
 * Note: HSTS (Strict-Transport-Security) and X-XSS-Protection headers are only sent
 * over HTTPS connections. Since these tests run over HTTP, we verify only the headers
 * that are sent regardless of the connection security.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({PostgresTestContainerConfiguration.class, RedisTestContainerConfiguration.class})
@Tag("integration")
class SecurityHeadersIntegrationTest extends IntegrationTestSupport {

    // Use the OpenAPI docs endpoint which is whitelisted and always available
    private static final String PUBLIC_ENDPOINT = "/v3/api-docs";
    private static final String PROTECTED_ENDPOINT = "/v1/api/templates/system";

    @BeforeEach
    void setUp() {
        initializeTestSupport();
    }

    @Test
    void responseContainsXFrameOptionsDeny() {
        var response = get(PUBLIC_ENDPOINT, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("X-Frame-Options")).isEqualTo("DENY");
    }

    @Test
    void responseContainsXContentTypeOptionsNosniff() {
        var response = get(PUBLIC_ENDPOINT, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
    }

    @Test
    void securityHeadersPresentOnUnauthorizedResponse() {
        // Even on 401 responses, security headers should be present
        var response = get(PROTECTED_ENDPOINT, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getFirst("X-Frame-Options")).isEqualTo("DENY");
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
    }
}
