package io.hephaistos.flagforge.customerapi.controller;

import io.hephaistos.flagforge.common.data.ApiKeyEntity;
import io.hephaistos.flagforge.common.data.ApplicationEntity;
import io.hephaistos.flagforge.common.data.CompanyEntity;
import io.hephaistos.flagforge.common.data.EnvironmentEntity;
import io.hephaistos.flagforge.common.data.TemplateEntity;
import io.hephaistos.flagforge.common.enums.KeyType;
import io.hephaistos.flagforge.common.enums.PricingTier;
import io.hephaistos.flagforge.common.enums.TemplateType;
import io.hephaistos.flagforge.common.types.StringTemplateField;
import io.hephaistos.flagforge.common.types.TemplateSchema;
import io.hephaistos.flagforge.customerapi.IntegrationTestSupport;
import io.hephaistos.flagforge.customerapi.PostgresTestContainerConfiguration;
import io.hephaistos.flagforge.customerapi.RedisTestContainerConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({PostgresTestContainerConfiguration.class, RedisTestContainerConfiguration.class})
@Tag("integration")
class RateLimitFilterIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private String testApiKey;

    @BeforeEach
    void setUp() {
        initializeTestSupport();
        // Generate unique API key per test to avoid Redis state pollution
        // API key must be 64 chars - use two UUIDs
        String uuid1 = UUID.randomUUID().toString().replace("-", "");
        String uuid2 = UUID.randomUUID().toString().replace("-", "");
        testApiKey = uuid1 + uuid2;
        transactionTemplate.execute(status -> {
            cleanupTestData();
            createTestData();
            return null;
        });
    }

    @Test
    void rateLimitHeadersArePresent() {
        var response = getWithApiKey("/v1/api/templates/system", testApiKey, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().get("X-RateLimit-Limit")).isNotNull();
        assertThat(response.getHeaders().get("X-RateLimit-Remaining")).isNotNull();
        assertThat(response.getHeaders().get("X-Monthly-Usage")).isNotNull();
        assertThat(response.getHeaders().get("X-Monthly-Limit")).isNotNull();
    }

    @Test
    void rateLimitHeadersShowCorrectValues() {
        var response = getWithApiKey("/v1/api/templates/system", testApiKey, String.class);

        // Rate limit is set to 3 in createTestData
        assertThat(response.getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("3");
        // Monthly limit is 100000
        assertThat(response.getHeaders().getFirst("X-Monthly-Limit")).isEqualTo("100000");
    }

    @Test
    void requestsWithinLimitSucceed() {
        // Make 3 requests (at the limit)
        for (int i = 0; i < 3; i++) {
            var response = getWithApiKey("/v1/api/templates/system", testApiKey, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    void requestsExceedingLimitReturn429() {
        // Exhaust the rate limit (3 requests)
        for (int i = 0; i < 3; i++) {
            getWithApiKey("/v1/api/templates/system", testApiKey, String.class);
        }

        // Fourth request should be rate limited
        var response = getWithApiKey("/v1/api/templates/system", testApiKey, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().get("Retry-After")).isNotNull();
    }

    @Test
    void rateLimitedResponseContainsRetryAfterHeader() {
        // Exhaust the rate limit
        for (int i = 0; i < 3; i++) {
            getWithApiKey("/v1/api/templates/system", testApiKey, String.class);
        }

        var response = getWithApiKey("/v1/api/templates/system", testApiKey, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        String retryAfter = response.getHeaders().getFirst("Retry-After");
        assertThat(retryAfter).isNotNull();
        // Retry-After should be a positive number (seconds)
        assertThat(Integer.parseInt(retryAfter)).isGreaterThanOrEqualTo(0);
    }

    @Test
    void monthlyUsageIncrementsWithEachRequest() {
        var response1 = getWithApiKey("/v1/api/templates/system", testApiKey, String.class);
        String usage1 = response1.getHeaders().getFirst("X-Monthly-Usage");

        var response2 = getWithApiKey("/v1/api/templates/system", testApiKey, String.class);
        String usage2 = response2.getHeaders().getFirst("X-Monthly-Usage");

        assertThat(Long.parseLong(usage2)).isEqualTo(Long.parseLong(usage1) + 1);
    }

    private void cleanupTestData() {
        entityManager.createNativeQuery("DELETE FROM template_values").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM template").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM api_key").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM environment").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM application").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM company").executeUpdate();
    }

    private void createTestData() {
        // Create company
        var company = new CompanyEntity();
        company.setName("Rate Limit Test Company");
        entityManager.persist(company);
        entityManager.flush();

        // Create application
        var application = new ApplicationEntity();
        application.setName("Rate Limit Test App");
        application.setCompanyId(company.getId());
        entityManager.persist(application);
        entityManager.flush();
        UUID applicationId = application.getId();

        // Create environment with low rate limit for testing
        var environment = new EnvironmentEntity();
        environment.setName("Rate Limited Env");
        environment.setDescription("Environment with low rate limit for testing");
        environment.setApplicationId(application.getId());
        environment.setTier(PricingTier.FREE);
        environment.setRateLimitRequestsPerSecond(3); // Low limit for easy testing
        environment.setRequestsPerMonth(100000);
        entityManager.persist(environment);
        entityManager.flush();

        // Create API key
        var apiKey = new ApiKeyEntity();
        apiKey.setApplicationId(application.getId());
        apiKey.setEnvironmentId(environment.getId());
        apiKey.setKey(testApiKey);
        apiKey.setKeyType(KeyType.READ);
        apiKey.setExpirationDate(OffsetDateTime.now().plusDays(30));
        entityManager.persist(apiKey);

        // Create SYSTEM template (required for the endpoint)
        var schema = new TemplateSchema(
                List.of(new StringTemplateField("test_field", "Test Field", false, "default", 0,
                        255)));
        var template = new TemplateEntity();
        template.setApplicationId(application.getId());
        template.setCompanyId(company.getId());
        template.setType(TemplateType.SYSTEM);
        template.setSchema(schema);
        entityManager.persist(template);

        entityManager.flush();
    }
}
