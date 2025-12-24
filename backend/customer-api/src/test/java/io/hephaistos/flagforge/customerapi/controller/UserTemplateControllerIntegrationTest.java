package io.hephaistos.flagforge.customerapi.controller;

import io.hephaistos.flagforge.common.data.ApiKeyEntity;
import io.hephaistos.flagforge.common.data.ApplicationEntity;
import io.hephaistos.flagforge.common.data.CompanyEntity;
import io.hephaistos.flagforge.common.data.EnvironmentEntity;
import io.hephaistos.flagforge.common.data.TemplateEntity;
import io.hephaistos.flagforge.common.data.TemplateValuesEntity;
import io.hephaistos.flagforge.common.data.UserTemplateValuesEntity;
import io.hephaistos.flagforge.common.enums.KeyType;
import io.hephaistos.flagforge.common.enums.PricingTier;
import io.hephaistos.flagforge.common.enums.TemplateType;
import io.hephaistos.flagforge.common.types.BooleanTemplateField;
import io.hephaistos.flagforge.common.types.StringTemplateField;
import io.hephaistos.flagforge.common.types.TemplateSchema;
import io.hephaistos.flagforge.common.util.UserIdHasher;
import io.hephaistos.flagforge.customerapi.IntegrationTestSupport;
import io.hephaistos.flagforge.customerapi.PostgresTestContainerConfiguration;
import io.hephaistos.flagforge.customerapi.RedisTestContainerConfiguration;
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
@Import({PostgresTestContainerConfiguration.class, RedisTestContainerConfiguration.class})
@Tag("integration")
class UserTemplateControllerIntegrationTest extends IntegrationTestSupport {

    private static final String READ_API_KEY =
            "read_key_0123456789abcdef0123456789abcdef0123456789abcdef012345";
    private static final String WRITE_API_KEY =
            "write_key_0123456789abcdef0123456789abcdef0123456789abcdef01234";

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
    void getUserTemplateValuesReturnsSchemaDefaults() {
        var response = getWithApiKey("/v1/api/templates/user/test-user", READ_API_KEY,
                MergedTemplateValuesResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().type()).isEqualTo(TemplateType.USER);
        assertThat(response.getBody().values()).containsEntry("theme", "light");
        assertThat(response.getBody().values()).containsEntry("notifications", true);
        assertThat(response.getBody().appliedIdentifier()).isEqualTo("test-user");
    }

    @Test
    void getUserTemplateValuesAppliesEnvironmentDefaults() {
        // Create environment-level default (identifier = "")
        transactionTemplate.execute(status -> {
            createEnvironmentDefault(Map.of("theme", "dark"));
            return null;
        });

        var response = getWithApiKey("/v1/api/templates/user/test-user", READ_API_KEY,
                MergedTemplateValuesResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Environment default overrides schema default
        assertThat(response.getBody().values()).containsEntry("theme", "dark");
        // Schema default still applies for other fields
        assertThat(response.getBody().values()).containsEntry("notifications", true);
    }

    @Test
    void getUserTemplateValuesAppliesUserOverrides() {
        // Create user-specific override
        transactionTemplate.execute(status -> {
            createUserOverride("specific-user", Map.of("theme", "blue", "notifications", false));
            return null;
        });

        var response = getWithApiKey("/v1/api/templates/user/specific-user", READ_API_KEY,
                MergedTemplateValuesResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().values()).containsEntry("theme", "blue");
        assertThat(response.getBody().values()).containsEntry("notifications", false);
    }

    @Test
    void getUserTemplateValuesMergesAllLayers() {
        // Create environment default
        transactionTemplate.execute(status -> {
            createEnvironmentDefault(Map.of("theme", "dark"));
            return null;
        });

        // Create user-specific override (only overrides notifications)
        transactionTemplate.execute(status -> {
            createUserOverride("layered-user", Map.of("notifications", false));
            return null;
        });

        var response = getWithApiKey("/v1/api/templates/user/layered-user", READ_API_KEY,
                MergedTemplateValuesResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Theme from environment default
        assertThat(response.getBody().values()).containsEntry("theme", "dark");
        // Notifications from user override
        assertThat(response.getBody().values()).containsEntry("notifications", false);
    }

    @Test
    void setUserTemplateValuesRequiresWriteKey() {
        var response = postWithApiKey("/v1/api/templates/user/test-user", READ_API_KEY,
                Map.of("theme", "custom"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void setUserTemplateValuesCreatesNewRecord() {
        var response = postWithApiKey("/v1/api/templates/user/new-user", WRITE_API_KEY,
                Map.of("theme", "custom"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify the values were saved
        var getResponse = getWithApiKey("/v1/api/templates/user/new-user", READ_API_KEY,
                MergedTemplateValuesResponse.class);
        assertThat(getResponse.getBody().values()).containsEntry("theme", "custom");
    }

    @Test
    void setUserTemplateValuesUpdatesExistingRecord() {
        // Create initial user override
        transactionTemplate.execute(status -> {
            createUserOverride("update-user", Map.of("theme", "initial"));
            return null;
        });

        // Update with new value
        var response = postWithApiKey("/v1/api/templates/user/update-user", WRITE_API_KEY,
                Map.of("theme", "updated"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify the values were updated
        var getResponse = getWithApiKey("/v1/api/templates/user/update-user", READ_API_KEY,
                MergedTemplateValuesResponse.class);
        assertThat(getResponse.getBody().values()).containsEntry("theme", "updated");
    }

    @Test
    void getUserTemplateValuesWorksWithReadKey() {
        var response = getWithApiKey("/v1/api/templates/user/any-user", READ_API_KEY,
                MergedTemplateValuesResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getUserTemplateValuesWorksWithWriteKey() {
        var response = getWithApiKey("/v1/api/templates/user/any-user", WRITE_API_KEY,
                MergedTemplateValuesResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getUserTemplateValuesWithoutApiKeyReturns401() {
        var response = get("/v1/api/templates/user/test-user", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void setUserTemplateValuesWithWriteKeySucceeds() {
        var response = postWithApiKey("/v1/api/templates/user/write-test", WRITE_API_KEY,
                Map.of("theme", "success"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void cleanupTestData() {
        entityManager.createNativeQuery("DELETE FROM user_template_values").executeUpdate();
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
        company.setName("User Template Test Company");
        entityManager.persist(company);
        entityManager.flush();

        // Create application
        var application = new ApplicationEntity();
        application.setName("User Template Test App");
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
        environment.setRateLimitRequestsPerSecond(100);
        environment.setRequestsPerMonth(100000);
        entityManager.persist(environment);
        entityManager.flush();
        environmentId = environment.getId();

        // Create READ API key
        var readApiKey = new ApiKeyEntity();
        readApiKey.setApplicationId(application.getId());
        readApiKey.setEnvironmentId(environment.getId());
        readApiKey.setKey(READ_API_KEY);
        readApiKey.setKeyType(KeyType.READ);
        readApiKey.setExpirationDate(OffsetDateTime.now().plusDays(30));
        entityManager.persist(readApiKey);

        // Create WRITE API key
        var writeApiKey = new ApiKeyEntity();
        writeApiKey.setApplicationId(application.getId());
        writeApiKey.setEnvironmentId(environment.getId());
        writeApiKey.setKey(WRITE_API_KEY);
        writeApiKey.setKeyType(KeyType.WRITE);
        writeApiKey.setExpirationDate(OffsetDateTime.now().plusDays(30));
        entityManager.persist(writeApiKey);

        // Create USER template
        var schema = new TemplateSchema(
                List.of(new StringTemplateField("theme", "Theme", true, "light", 0, 50),
                        new BooleanTemplateField("notifications", "Notifications Enabled", true,
                                true)));
        var template = new TemplateEntity();
        template.setApplicationId(application.getId());
        template.setCompanyId(company.getId());
        template.setType(TemplateType.USER);
        template.setSchema(schema);
        entityManager.persist(template);

        entityManager.flush();
    }

    private void createEnvironmentDefault(Map<String, Object> values) {
        var override = new TemplateValuesEntity();
        override.setApplicationId(applicationId);
        override.setEnvironmentId(environmentId);
        override.setType(TemplateType.USER);
        override.setIdentifier(""); // Empty string for environment-level default
        override.setValues(values);
        entityManager.persist(override);
        entityManager.flush();
    }

    private void createUserOverride(String userId, Map<String, Object> values) {
        var override = new UserTemplateValuesEntity();
        override.setApplicationId(applicationId);
        override.setEnvironmentId(environmentId);
        override.setUserId(UserIdHasher.toUuid(userId));
        override.setValues(values);
        entityManager.persist(override);
        entityManager.flush();
    }
}
