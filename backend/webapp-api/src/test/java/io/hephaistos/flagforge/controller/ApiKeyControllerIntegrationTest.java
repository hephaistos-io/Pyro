package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.IntegrationTestSupport;
import io.hephaistos.flagforge.PostgresTestContainerConfiguration;
import io.hephaistos.flagforge.controller.dto.ApiKeyResponse;
import io.hephaistos.flagforge.controller.dto.ApplicationCreationRequest;
import io.hephaistos.flagforge.controller.dto.ApplicationResponse;
import io.hephaistos.flagforge.controller.dto.EnvironmentCreationRequest;
import io.hephaistos.flagforge.controller.dto.EnvironmentResponse;
import io.hephaistos.flagforge.data.KeyType;
import io.hephaistos.flagforge.data.repository.ApiKeyRepository;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.data.repository.CompanyRepository;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.data.repository.EnvironmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestContainerConfiguration.class)
@Tag("integration")
class ApiKeyControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @BeforeEach
    void beforeEach() {
        initializeTestSupport();
        apiKeyRepository.deleteAll();
        environmentRepository.deleteAll();
        applicationRepository.deleteAll();
        companyRepository.deleteAll();
        customerRepository.deleteAll();
    }

    // ========== Get API Key by Type ==========

    @Test
    void getApiKeyByTypeReturnsReadKey() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");
        UUID environmentId = getDefaultEnvironmentId(token, applicationId);

        var response = get(apiKeyPath(applicationId, environmentId, KeyType.READ), token,
                ApiKeyResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().keyType()).isEqualTo(KeyType.READ);
        assertThat(response.getBody().environmentId()).isEqualTo(environmentId);
        assertThat(response.getBody().rateLimitRequestsPerMinute()).isEqualTo(1000);
        assertThat(response.getBody().secretKey()).isNotNull();
        assertThat(response.getBody().secretKey()).hasSize(64);
    }

    @Test
    void getApiKeyByTypeReturnsWriteKey() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");
        UUID environmentId = getDefaultEnvironmentId(token, applicationId);

        var response = get(apiKeyPath(applicationId, environmentId, KeyType.WRITE), token,
                ApiKeyResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().keyType()).isEqualTo(KeyType.WRITE);
        assertThat(response.getBody().environmentId()).isEqualTo(environmentId);
    }

    @Test
    void getApiKeyByTypeReturns404ForNonExistentApplication() {
        String token = registerAndAuthenticateWithCompany();
        UUID nonExistentAppId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();

        var response =
                get(apiKeyPath(nonExistentAppId, environmentId, KeyType.READ), token, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getApiKeyByTypeReturns404ForNonExistentEnvironment() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");
        UUID nonExistentEnvId = UUID.randomUUID();

        var response =
                get(apiKeyPath(applicationId, nonExistentEnvId, KeyType.READ), token, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ========== Automatic API Key Creation ==========

    @Test
    void apiKeysAreAutomaticallyCreatedForDefaultEnvironment() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");
        UUID environmentId = getDefaultEnvironmentId(token, applicationId);

        // Both READ and WRITE keys should exist
        var readKeyResponse = get(apiKeyPath(applicationId, environmentId, KeyType.READ), token,
                ApiKeyResponse.class);
        var writeKeyResponse = get(apiKeyPath(applicationId, environmentId, KeyType.WRITE), token,
                ApiKeyResponse.class);

        assertThat(readKeyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(writeKeyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(readKeyResponse.getBody().id()).isNotEqualTo(writeKeyResponse.getBody().id());
    }

    @Test
    void apiKeysAreAutomaticallyCreatedForNewEnvironment() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");

        // Create a new environment
        var envResponse = post("/v1/applications/" + applicationId + "/environments",
                new EnvironmentCreationRequest("Production", "Prod env"), token,
                EnvironmentResponse.class);
        UUID productionEnvId = envResponse.getBody().id();

        // Both READ and WRITE keys should exist for the new environment
        var readKeyResponse = get(apiKeyPath(applicationId, productionEnvId, KeyType.READ), token,
                ApiKeyResponse.class);
        var writeKeyResponse = get(apiKeyPath(applicationId, productionEnvId, KeyType.WRITE), token,
                ApiKeyResponse.class);

        assertThat(readKeyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(writeKeyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(readKeyResponse.getBody().environmentId()).isEqualTo(productionEnvId);
        assertThat(writeKeyResponse.getBody().environmentId()).isEqualTo(productionEnvId);
    }

    @Test
    void eachEnvironmentHasDistinctApiKeys() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");
        UUID defaultEnvId = getDefaultEnvironmentId(token, applicationId);

        // Create a second environment
        var envResponse = post("/v1/applications/" + applicationId + "/environments",
                new EnvironmentCreationRequest("Staging", "Staging env"), token,
                EnvironmentResponse.class);
        UUID stagingEnvId = envResponse.getBody().id();

        // Get keys for both environments
        var defaultReadKey = get(apiKeyPath(applicationId, defaultEnvId, KeyType.READ), token,
                ApiKeyResponse.class).getBody();
        var stagingReadKey = get(apiKeyPath(applicationId, stagingEnvId, KeyType.READ), token,
                ApiKeyResponse.class).getBody();

        // Keys should be different
        assertThat(defaultReadKey.id()).isNotEqualTo(stagingReadKey.id());
    }

    // ========== Regenerate API Key ==========

    @Test
    void regenerateApiKeyReturnsNewKey() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");
        UUID environmentId = getDefaultEnvironmentId(token, applicationId);

        // Get the original key
        var originalKey = get(apiKeyPath(applicationId, environmentId, KeyType.READ), token,
                ApiKeyResponse.class).getBody();

        // Regenerate by key type
        var regenerateResponse =
                post(apiKeyPath(applicationId, environmentId, KeyType.READ) + "/regenerate", null,
                        token, ApiKeyResponse.class);

        assertThat(regenerateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(regenerateResponse.getBody()).isNotNull();
        assertThat(regenerateResponse.getBody().id()).isNotEqualTo(originalKey.id());
        assertThat(regenerateResponse.getBody().secretKey()).isNotNull();
        assertThat(regenerateResponse.getBody().secretKey()).hasSize(64);
        assertThat(regenerateResponse.getBody().secretKey()).isNotEqualTo(originalKey.secretKey());
        assertThat(regenerateResponse.getBody().expirationDate()).isAfter(
                java.time.OffsetDateTime.now());
        assertThat(regenerateResponse.getBody().expirationDate()).isBefore(
                java.time.OffsetDateTime.now().plusWeeks(1).plusMinutes(1));
    }

    @Test
    void regenerateApiKeyCreatesNewKeyEntity() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");
        UUID environmentId = getDefaultEnvironmentId(token, applicationId);

        var originalKey = get(apiKeyPath(applicationId, environmentId, KeyType.WRITE), token,
                ApiKeyResponse.class).getBody();

        post(apiKeyPath(applicationId, environmentId, KeyType.WRITE) + "/regenerate", null, token,
                ApiKeyResponse.class);

        // Verify a new key was created with a different ID
        var newKey = get(apiKeyPath(applicationId, environmentId, KeyType.WRITE), token,
                ApiKeyResponse.class).getBody();

        assertThat(newKey.id()).isNotEqualTo(originalKey.id());
        assertThat(newKey.secretKey()).isNotEqualTo(originalKey.secretKey());
    }

    @Test
    void regenerateApiKeyExpiresOldKeyAfterOneWeek() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");
        UUID environmentId = getDefaultEnvironmentId(token, applicationId);

        var originalKey = get(apiKeyPath(applicationId, environmentId, KeyType.READ), token,
                ApiKeyResponse.class).getBody();
        UUID originalKeyId = originalKey.id();

        post(apiKeyPath(applicationId, environmentId, KeyType.READ) + "/regenerate", null, token,
                ApiKeyResponse.class);

        // Verify the old key still exists in DB but will expire in one week
        var oldKeyEntity = apiKeyRepository.findById(originalKeyId);
        assertThat(oldKeyEntity).isPresent();
        assertThat(oldKeyEntity.get().getExpirationDate()).isAfter(java.time.OffsetDateTime.now());
        assertThat(oldKeyEntity.get().getExpirationDate()).isBefore(
                java.time.OffsetDateTime.now().plusWeeks(1).plusMinutes(1));
    }

    @Test
    void regenerateApiKeyReturns404ForNonExistentApplication() {
        String token = registerAndAuthenticateWithCompany();
        UUID nonExistentAppId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();

        var response =
                post(apiKeyPath(nonExistentAppId, environmentId, KeyType.READ) + "/regenerate",
                        null, token, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void regenerateApiKeyReturns404ForNonExistentEnvironment() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");
        UUID nonExistentEnvId = UUID.randomUUID();

        var response =
                post(apiKeyPath(applicationId, nonExistentEnvId, KeyType.READ) + "/regenerate",
                        null, token, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ========== Multi-tenancy Isolation ==========

    @Test
    void cannotAccessApiKeysFromOtherCompany() {
        // Create user 1 with company A
        registerUser("User", "One", "user1@example.com", "password123");
        String token1 = authenticate("user1@example.com", "password123");
        createCompany(token1, "Company A");
        token1 = authenticate("user1@example.com", "password123");
        UUID appIdA = createApplication(token1, "App A");
        UUID envIdA = getDefaultEnvironmentId(token1, appIdA);

        // Create user 2 with company B
        registerUser("User", "Two", "user2@example.com", "password123");
        String token2 = authenticate("user2@example.com", "password123");
        createCompany(token2, "Company B");
        token2 = authenticate("user2@example.com", "password123");

        // User 2 should not be able to access keys from Company A
        var response = get(apiKeyPath(appIdA, envIdA, KeyType.READ), token2, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cannotRegenerateApiKeysFromOtherCompany() {
        // Create user 1 with company A
        registerUser("User", "One", "user1@example.com", "password123");
        String token1 = authenticate("user1@example.com", "password123");
        createCompany(token1, "Company A");
        token1 = authenticate("user1@example.com", "password123");
        UUID appIdA = createApplication(token1, "App A");
        UUID envIdA = getDefaultEnvironmentId(token1, appIdA);

        // Create user 2 with company B
        registerUser("User", "Two", "user2@example.com", "password123");
        String token2 = authenticate("user2@example.com", "password123");
        createCompany(token2, "Company B");
        token2 = authenticate("user2@example.com", "password123");

        // User 2 should not be able to regenerate keys from Company A's app
        var response = post(apiKeyPath(appIdA, envIdA, KeyType.READ) + "/regenerate", null, token2,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ========== API Keys Deleted with Environment ==========

    @Test
    void apiKeysAreDeletedWhenEnvironmentIsDeleted() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");

        // Create a deletable (PAID tier) environment
        var envResponse = post("/v1/applications/" + applicationId + "/environments",
                new EnvironmentCreationRequest("Production", "Prod"), token,
                EnvironmentResponse.class);
        UUID envId = envResponse.getBody().id();

        // Verify keys exist
        var readKey =
                get(apiKeyPath(applicationId, envId, KeyType.READ), token, ApiKeyResponse.class);
        assertThat(readKey.getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID readKeyId = readKey.getBody().id();

        // Delete the environment
        delete("/v1/applications/" + applicationId + "/environments/" + envId, token, Void.class);

        // Verify keys are deleted (via repository since endpoint will 404)
        assertThat(apiKeyRepository.existsById(readKeyId)).isFalse();
    }

    // ========== Helper Methods ==========

    private UUID createApplication(String token, String name) {
        var response = post("/v1/applications", new ApplicationCreationRequest(name), token,
                ApplicationResponse.class);
        return response.getBody().id();
    }

    private UUID getDefaultEnvironmentId(String token, UUID applicationId) {
        var response = get("/v1/applications/" + applicationId + "/environments", token,
                EnvironmentResponse[].class);
        return response.getBody()[0].id();
    }

    private String apiKeyBasePath(UUID applicationId, UUID environmentId) {
        return "/v1/applications/" + applicationId + "/environments/" + environmentId + "/api-keys";
    }

    private String apiKeyPath(UUID applicationId, UUID environmentId, KeyType keyType) {
        return apiKeyBasePath(applicationId, environmentId) + "/" + keyType;
    }
}
