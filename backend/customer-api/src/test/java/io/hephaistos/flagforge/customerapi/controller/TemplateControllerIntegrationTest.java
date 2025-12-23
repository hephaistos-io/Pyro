package io.hephaistos.flagforge.customerapi.controller;

import io.hephaistos.flagforge.common.data.ApiKeyEntity;
import io.hephaistos.flagforge.common.data.ApplicationEntity;
import io.hephaistos.flagforge.common.data.CompanyEntity;
import io.hephaistos.flagforge.common.data.EnvironmentEntity;
import io.hephaistos.flagforge.common.data.TemplateEntity;
import io.hephaistos.flagforge.common.data.TemplateValuesEntity;
import io.hephaistos.flagforge.common.enums.KeyType;
import io.hephaistos.flagforge.common.enums.PricingTier;
import io.hephaistos.flagforge.common.enums.TemplateType;
import io.hephaistos.flagforge.common.types.StringTemplateField;
import io.hephaistos.flagforge.common.types.TemplateSchema;
import io.hephaistos.flagforge.customerapi.IntegrationTestSupport;
import io.hephaistos.flagforge.customerapi.PostgresTestContainerConfiguration;
import io.hephaistos.flagforge.customerapi.controller.dto.MergedTemplateValuesResponse;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@Tag("integration")
class TemplateControllerIntegrationTest extends IntegrationTestSupport {

    private static final String TEST_API_KEY =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private UUID applicationId;
    private UUID environmentId;

    @BeforeEach
    void setUp() {
        initializeTestSupport();
        transactionTemplate.execute(status -> {
            cleanupTestData();
            createTestData();
            return null;
        });
    }

    @Test
    void getSystemTemplateValuesReturnsDefaults() {
        var response = getWithApiKey("/v1/api/templates/system", TEST_API_KEY,
                MergedTemplateValuesResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().type()).isEqualTo(TemplateType.SYSTEM);
        assertThat(response.getBody().values()).containsEntry("api_url", "https://default.api.com");
        assertThat(response.getBody().appliedIdentifier()).isNull();
    }

    @Test
    void getSystemTemplateValuesWithIdentifierAppliesOverride() {
        // Create an override
        transactionTemplate.execute(status -> {
            createOverride("region-eu", Map.of("api_url", "https://eu.api.com"));
            return null;
        });

        var response = getWithApiKey("/v1/api/templates/system?identifier=region-eu", TEST_API_KEY,
                MergedTemplateValuesResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().values()).containsEntry("api_url", "https://eu.api.com");
        assertThat(response.getBody().appliedIdentifier()).isEqualTo("region-eu");
    }

    @Test
    void getSystemTemplateValuesWithNonExistentIdentifierReturnsDefaults() {
        var response =
                getWithApiKey("/v1/api/templates/system?identifier=non-existent", TEST_API_KEY,
                        MergedTemplateValuesResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().values()).containsEntry("api_url", "https://default.api.com");
        assertThat(response.getBody().appliedIdentifier()).isNull();
    }

    @Test
    void getSystemTemplateValuesWithoutApiKeyReturns401() {
        var response = get("/v1/api/templates/system", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getSystemTemplateValuesWithInvalidApiKeyReturns401() {
        var response = getWithApiKey("/v1/api/templates/system", "invalid-key", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getSystemTemplateValuesWithExpiredApiKeyReturns401() {
        // Create an expired API key
        transactionTemplate.execute(status -> {
            var expiredKey = new ApiKeyEntity();
            expiredKey.setApplicationId(applicationId);
            expiredKey.setEnvironmentId(environmentId);
            expiredKey.setKey("expired_key_123456789012345678901234567890123456789012345678901");
            expiredKey.setKeyType(KeyType.READ);
            expiredKey.setExpirationDate(OffsetDateTime.now().minusDays(1));
            entityManager.persist(expiredKey);
            return null;
        });

        var response = getWithApiKey("/v1/api/templates/system",
                "expired_key_123456789012345678901234567890123456789012345678901", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getSystemTemplateValuesIncludesSchema() {
        var response = getWithApiKey("/v1/api/templates/system", TEST_API_KEY,
                MergedTemplateValuesResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().schema()).isNotNull();
        assertThat(response.getBody().schema().fields()).hasSize(2);
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
        company.setName("Test Company");
        entityManager.persist(company);
        entityManager.flush();

        // Create application
        var application = new ApplicationEntity();
        application.setName("Test App");
        application.setCompanyId(company.getId());
        entityManager.persist(application);
        entityManager.flush();
        applicationId = application.getId();

        // Create environment
        var environment = new EnvironmentEntity();
        environment.setName("Production");
        environment.setDescription("Production environment");
        environment.setApplicationId(application.getId());
        environment.setTier(PricingTier.BASIC);
        environment.setRateLimitRequestsPerSecond(10);
        environment.setRequestsPerMonth(100000);
        entityManager.persist(environment);
        entityManager.flush();
        environmentId = environment.getId();

        // Create API key
        var apiKey = new ApiKeyEntity();
        apiKey.setApplicationId(application.getId());
        apiKey.setEnvironmentId(environment.getId());
        apiKey.setKey(TEST_API_KEY);
        apiKey.setKeyType(KeyType.READ);
        apiKey.setExpirationDate(OffsetDateTime.now().plusDays(30));
        entityManager.persist(apiKey);

        // Create SYSTEM template
        var schema = new TemplateSchema(List.of(new StringTemplateField("api_url", "API URL", false,
                        "https://default.api.com", 0, 255),
                new StringTemplateField("region", "Region", false, "us-east", 0, 50)));
        var template = new TemplateEntity();
        template.setApplicationId(application.getId());
        template.setCompanyId(company.getId());
        template.setType(TemplateType.SYSTEM);
        template.setSchema(schema);
        entityManager.persist(template);

        entityManager.flush();
    }

    private void createOverride(String identifier, Map<String, Object> values) {
        var override = new TemplateValuesEntity();
        override.setApplicationId(applicationId);
        override.setEnvironmentId(environmentId);
        override.setType(TemplateType.SYSTEM);
        override.setIdentifier(identifier);
        override.setValues(values);
        entityManager.persist(override);
        entityManager.flush();
    }
}
