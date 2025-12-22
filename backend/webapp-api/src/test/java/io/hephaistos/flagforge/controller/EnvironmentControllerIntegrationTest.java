package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.IntegrationTestSupport;
import io.hephaistos.flagforge.PostgresTestContainerConfiguration;
import io.hephaistos.flagforge.common.enums.PricingTier;
import io.hephaistos.flagforge.controller.dto.ApplicationCreationRequest;
import io.hephaistos.flagforge.controller.dto.ApplicationResponse;
import io.hephaistos.flagforge.controller.dto.EnvironmentCreationRequest;
import io.hephaistos.flagforge.controller.dto.EnvironmentResponse;
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
class EnvironmentControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @BeforeEach
    void beforeEach() {
        initializeTestSupport();
        environmentRepository.deleteAll();
        applicationRepository.deleteAll();
        companyRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    void createEnvironmentReturns201WithValidRequest() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");

        var response = post("/v1/applications/" + applicationId + "/environments",
                new EnvironmentCreationRequest("Staging", "Staging environment"), token,
                EnvironmentResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Staging");
        assertThat(response.getBody().description()).isEqualTo("Staging environment");
        assertThat(response.getBody().tier()).isEqualTo(PricingTier.BASIC);
    }

    @Test
    void createEnvironmentReturns409ForDuplicateName() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");

        // Create first environment
        post("/v1/applications/" + applicationId + "/environments",
                new EnvironmentCreationRequest("Production", "First"), token,
                EnvironmentResponse.class);

        // Try to create duplicate
        var response = post("/v1/applications/" + applicationId + "/environments",
                new EnvironmentCreationRequest("Production", "Second"), token, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("DUPLICATE_RESOURCE");
    }

    @Test
    void getEnvironmentsReturnsListIncludingDefaultEnvironment() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");

        // Create additional environment
        post("/v1/applications/" + applicationId + "/environments",
                new EnvironmentCreationRequest("Staging", "Staging env"), token,
                EnvironmentResponse.class);

        var response = get("/v1/applications/" + applicationId + "/environments", token,
                EnvironmentResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3); // Development + Production (defaults) + Staging
    }

    @Test
    void deleteEnvironmentReturns204ForBasicTier() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");

        // Create a BASIC tier environment
        var createResponse = post("/v1/applications/" + applicationId + "/environments",
                new EnvironmentCreationRequest("Staging", "Staging env"), token,
                EnvironmentResponse.class);
        UUID environmentId = createResponse.getBody().id();

        // Delete it
        var deleteResponse =
                delete("/v1/applications/" + applicationId + "/environments/" + environmentId,
                        token, Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify it's actually deleted
        var listResponse = get("/v1/applications/" + applicationId + "/environments", token,
                EnvironmentResponse[].class);

        assertThat(listResponse.getBody()).hasSize(
                2); // Only default environments remain (Development + Production)
        assertThat(listResponse.getBody()[0].tier()).isEqualTo(PricingTier.FREE);
        assertThat(listResponse.getBody()[1].tier()).isEqualTo(PricingTier.FREE);
    }

    @Test
    void deleteEnvironmentReturns403ForFreeTier() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");

        // Get the default FREE tier environment
        var listResponse = get("/v1/applications/" + applicationId + "/environments", token,
                EnvironmentResponse[].class);
        UUID freeEnvironmentId = listResponse.getBody()[0].id();

        // Try to delete it
        var deleteResponse =
                delete("/v1/applications/" + applicationId + "/environments/" + freeEnvironmentId,
                        token, String.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(deleteResponse.getBody()).contains("OPERATION_NOT_ALLOWED");
    }

    @Test
    void deleteEnvironmentThenRecreateWithSameNameSucceeds() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");

        // Create environment
        var createResponse = post("/v1/applications/" + applicationId + "/environments",
                new EnvironmentCreationRequest("Staging", "Staging environment"), token,
                EnvironmentResponse.class);
        UUID environmentId = createResponse.getBody().id();

        // Delete it
        var deleteResponse =
                delete("/v1/applications/" + applicationId + "/environments/" + environmentId,
                        token, Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify deletion in database
        assertThat(environmentRepository.existsById(environmentId)).isFalse();

        // Recreate with same name
        var recreateResponse = post("/v1/applications/" + applicationId + "/environments",
                new EnvironmentCreationRequest("Staging", "New staging environment"), token,
                EnvironmentResponse.class);

        assertThat(recreateResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(recreateResponse.getBody().name()).isEqualTo("Staging");
        assertThat(recreateResponse.getBody().id()).isNotEqualTo(environmentId);
    }

    @Test
    void deleteEnvironmentReturns404ForNonExistentEnvironment() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");
        UUID nonExistentId = UUID.randomUUID();

        var response =
                delete("/v1/applications/" + applicationId + "/environments/" + nonExistentId,
                        token, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteEnvironmentReturns404ForEnvironmentInDifferentApplication() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId1 = createApplication(token, "App 1");
        UUID applicationId2 = createApplication(token, "App 2");

        // Create environment in app 1
        var createResponse = post("/v1/applications/" + applicationId1 + "/environments",
                new EnvironmentCreationRequest("Staging", "Staging"), token,
                EnvironmentResponse.class);
        UUID environmentId = createResponse.getBody().id();

        // Try to delete via app 2
        var deleteResponse =
                delete("/v1/applications/" + applicationId2 + "/environments/" + environmentId,
                        token, String.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void environmentsAreIsolatedBetweenCompanies() {
        // Create user 1 with company A and application
        registerUser("User", "One", "user1@example.com", "password123");
        String token1 = authenticate("user1@example.com", "password123");
        createCompany(token1, "Company A");
        token1 = authenticate("user1@example.com", "password123");
        UUID appIdA = createApplication(token1, "App A");

        // Create user 2 with company B and application
        registerUser("User", "Two", "user2@example.com", "password123");
        String token2 = authenticate("user2@example.com", "password123");
        createCompany(token2, "Company B");
        token2 = authenticate("user2@example.com", "password123");
        UUID appIdB = createApplication(token2, "App B");

        // User 1 should not be able to access environments for Company B's app
        var responseUser1AccessAppB =
                get("/v1/applications/" + appIdB + "/environments", token1, String.class);
        // Should return 404 because the application is not accessible
        assertThat(responseUser1AccessAppB.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // User 2 should not be able to access environments for Company A's app
        var responseUser2AccessAppA =
                get("/v1/applications/" + appIdA + "/environments", token2, String.class);
        // Should return 404 because the application is not accessible
        assertThat(responseUser2AccessAppA.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cannotCreateEnvironmentInInaccessibleApplication() {
        // Create user 1 with company A and application
        registerUser("User", "One", "user1@example.com", "password123");
        String token1 = authenticate("user1@example.com", "password123");
        createCompany(token1, "Company A");
        token1 = authenticate("user1@example.com", "password123");
        UUID appIdA = createApplication(token1, "App A");

        // Create user 2 with company B
        registerUser("User", "Two", "user2@example.com", "password123");
        String token2 = authenticate("user2@example.com", "password123");
        createCompany(token2, "Company B");
        token2 = authenticate("user2@example.com", "password123");

        // User 2 should not be able to create environment in Company A's app
        var response = post("/v1/applications/" + appIdA + "/environments",
                new EnvironmentCreationRequest("Staging", "Staging env"), token2, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private UUID createApplication(String token, String name) {
        var response = post("/v1/applications", new ApplicationCreationRequest(name), token,
                ApplicationResponse.class);
        return response.getBody().id();
    }
}
