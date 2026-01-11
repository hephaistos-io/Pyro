package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.IntegrationTestSupport;
import io.hephaistos.flagforge.MailpitTestConfiguration;
import io.hephaistos.flagforge.PostgresTestContainerConfiguration;
import io.hephaistos.flagforge.RedisTestContainerConfiguration;
import io.hephaistos.flagforge.common.enums.CustomerRole;
import io.hephaistos.flagforge.common.enums.PricingTier;
import io.hephaistos.flagforge.controller.dto.ApplicationCreationRequest;
import io.hephaistos.flagforge.controller.dto.ApplicationResponse;
import io.hephaistos.flagforge.controller.dto.EnvironmentCreationRequest;
import io.hephaistos.flagforge.controller.dto.EnvironmentResponse;
import io.hephaistos.flagforge.controller.dto.EnvironmentUpdateRequest;
import io.hephaistos.flagforge.controller.dto.InviteCreationRequest;
import io.hephaistos.flagforge.controller.dto.InviteCreationResponse;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.data.repository.CompanyInviteRepository;
import io.hephaistos.flagforge.data.repository.CompanyRepository;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.data.repository.EnvironmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestContainerConfiguration.class, RedisTestContainerConfiguration.class,
        MailpitTestConfiguration.class})
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

    @Autowired
    private CompanyInviteRepository companyInviteRepository;

    @BeforeEach
    void beforeEach() {
        initializeTestSupport();
        companyInviteRepository.deleteAll();
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
                new EnvironmentCreationRequest("Staging", "Staging environment", PricingTier.BASIC),
                token,
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
                new EnvironmentCreationRequest("Production", "First", PricingTier.BASIC), token,
                EnvironmentResponse.class);

        // Try to create duplicate
        var response = post("/v1/applications/" + applicationId + "/environments",
                new EnvironmentCreationRequest("Production", "Second", PricingTier.BASIC), token,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("DUPLICATE_RESOURCE");
    }

    @Test
    void getEnvironmentsReturnsListIncludingDefaultEnvironment() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");

        // Create additional environment
        post("/v1/applications/" + applicationId + "/environments",
                new EnvironmentCreationRequest("Staging", "Staging env", PricingTier.BASIC), token,
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
                new EnvironmentCreationRequest("Staging", "Staging env", PricingTier.BASIC), token,
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
    void deleteEnvironmentReturns204ForFreeTier() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");

        // Get the default FREE tier environment
        var listResponse = get("/v1/applications/" + applicationId + "/environments", token,
                EnvironmentResponse[].class);
        UUID freeEnvironmentId = listResponse.getBody()[0].id();

        // Delete it - FREE tier environments CAN be deleted
        var deleteResponse =
                delete("/v1/applications/" + applicationId + "/environments/" + freeEnvironmentId,
                        token, Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void deleteEnvironmentThenRecreateWithSameNameSucceeds() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");

        // Create environment
        var createResponse = post("/v1/applications/" + applicationId + "/environments",
                new EnvironmentCreationRequest("Staging", "Staging environment", PricingTier.BASIC),
                token,
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
                new EnvironmentCreationRequest("Staging", "New staging environment",
                        PricingTier.BASIC), token,
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
                new EnvironmentCreationRequest("Staging", "Staging", PricingTier.BASIC), token,
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
                new EnvironmentCreationRequest("Staging", "Staging env", PricingTier.BASIC), token2,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private UUID createApplication(String token, String name) {
        var response = post("/v1/applications", new ApplicationCreationRequest(name), token,
                ApplicationResponse.class);
        return response.getBody().id();
    }

    private String createUserWithRoleAndAppAccess(String adminToken, CustomerRole role,
            UUID applicationId) {
        String email = UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        var inviteRequest = new InviteCreationRequest(email, role, Set.of(applicationId), null);
        var inviteResponse =
                post("/v1/company/invite", inviteRequest, adminToken, InviteCreationResponse.class);
        assertThat(inviteResponse.getStatusCode()).as(
                "Invite creation should succeed for email: " + email).isEqualTo(HttpStatus.CREATED);

        String inviteToken = getInviteTokenByEmail(email);
        registerUserWithInvite("Test", "User", "password123", inviteToken);
        return authenticate(email, "password123");
    }

    private String getInviteTokenByEmail(String email) {
        return companyInviteRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invite not found for email: " + email))
                .getToken();
    }

    @Nested
    @DisplayName("Role-based Access Control")
    class RoleBasedAccessControl {

        @Test
        void adminRoleCanCreateEnvironment() {
            // Setup: Admin creates company and application
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();
            UUID applicationId = createApplication(adminToken, "Test App");

            // Admin should be able to create environment
            var response = post("/v1/applications/" + applicationId + "/environments",
                    new EnvironmentCreationRequest("Staging", "Staging env", PricingTier.BASIC),
                    adminToken, EnvironmentResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().name()).isEqualTo("Staging");
        }

        @Test
        void devRoleCannotCreateEnvironment() {
            // Setup: Admin creates company and application
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();
            UUID applicationId = createApplication(adminToken, "Test App");

            // Create DEV user in the same company with app access
            String devToken =
                    createUserWithRoleAndAppAccess(adminToken, CustomerRole.DEV, applicationId);

            // DEV should NOT be able to create environment (admin only)
            var response = post("/v1/applications/" + applicationId + "/environments",
                    new EnvironmentCreationRequest("Staging", "Staging env", PricingTier.BASIC),
                    devToken, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void readOnlyRoleCannotCreateEnvironment() {
            // Setup: Admin creates company and application
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();
            UUID applicationId = createApplication(adminToken, "Test App");

            // Create READ_ONLY user with app access
            String readOnlyToken =
                    createUserWithRoleAndAppAccess(adminToken, CustomerRole.READ_ONLY,
                            applicationId);

            // READ_ONLY should NOT be able to create environment
            var response = post("/v1/applications/" + applicationId + "/environments",
                    new EnvironmentCreationRequest("Staging", "Staging env", PricingTier.BASIC),
                    readOnlyToken, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void adminRoleCanUpdateEnvironment() {
            // Setup: Admin creates company, application and environment
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();
            UUID applicationId = createApplication(adminToken, "Test App");

            var createResponse = post("/v1/applications/" + applicationId + "/environments",
                    new EnvironmentCreationRequest("Staging", "Staging env", PricingTier.BASIC),
                    adminToken, EnvironmentResponse.class);
            UUID environmentId = createResponse.getBody().id();

            // Admin should be able to update environment
            var response =
                    put("/v1/applications/" + applicationId + "/environments/" + environmentId,
                            new EnvironmentUpdateRequest("Updated Staging", "Updated desc"),
                            adminToken, EnvironmentResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().name()).isEqualTo("Updated Staging");
        }

        @Test
        void devRoleCannotUpdateEnvironment() {
            // Setup: Admin creates company, application and environment
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();
            UUID applicationId = createApplication(adminToken, "Test App");

            var createResponse = post("/v1/applications/" + applicationId + "/environments",
                    new EnvironmentCreationRequest("Staging", "Staging env", PricingTier.BASIC),
                    adminToken, EnvironmentResponse.class);
            UUID environmentId = createResponse.getBody().id();

            // Create DEV user with app access
            String devToken =
                    createUserWithRoleAndAppAccess(adminToken, CustomerRole.DEV, applicationId);

            // DEV should NOT be able to update environment (admin only)
            var response =
                    put("/v1/applications/" + applicationId + "/environments/" + environmentId,
                            new EnvironmentUpdateRequest("Updated Staging", "Updated desc"),
                            devToken, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void readOnlyRoleCannotUpdateEnvironment() {
            // Setup: Admin creates company, application and environment
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();
            UUID applicationId = createApplication(adminToken, "Test App");

            var createResponse = post("/v1/applications/" + applicationId + "/environments",
                    new EnvironmentCreationRequest("Staging", "Staging env", PricingTier.BASIC),
                    adminToken, EnvironmentResponse.class);
            UUID environmentId = createResponse.getBody().id();

            // Create READ_ONLY user with app access
            String readOnlyToken =
                    createUserWithRoleAndAppAccess(adminToken, CustomerRole.READ_ONLY,
                            applicationId);

            // READ_ONLY should NOT be able to update environment
            var response =
                    put("/v1/applications/" + applicationId + "/environments/" + environmentId,
                            new EnvironmentUpdateRequest("Updated Staging", "Updated desc"),
                            readOnlyToken, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void adminRoleCanDeleteEnvironment() {
            // Setup: Admin creates company, application and environment
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();
            UUID applicationId = createApplication(adminToken, "Test App");

            var createResponse = post("/v1/applications/" + applicationId + "/environments",
                    new EnvironmentCreationRequest("Staging", "Staging env", PricingTier.BASIC),
                    adminToken, EnvironmentResponse.class);
            UUID environmentId = createResponse.getBody().id();

            // Admin should be able to delete environment
            var response =
                    delete("/v1/applications/" + applicationId + "/environments/" + environmentId,
                            adminToken, Void.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        void devRoleCannotDeleteEnvironment() {
            // Setup: Admin creates company, application and environment
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();
            UUID applicationId = createApplication(adminToken, "Test App");

            var createResponse = post("/v1/applications/" + applicationId + "/environments",
                    new EnvironmentCreationRequest("Staging", "Staging env", PricingTier.BASIC),
                    adminToken, EnvironmentResponse.class);
            UUID environmentId = createResponse.getBody().id();

            // Create DEV user with app access
            String devToken =
                    createUserWithRoleAndAppAccess(adminToken, CustomerRole.DEV, applicationId);

            // DEV should NOT be able to delete environment (admin only)
            var response =
                    delete("/v1/applications/" + applicationId + "/environments/" + environmentId,
                            devToken, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void readOnlyRoleCannotDeleteEnvironment() {
            // Setup: Admin creates company, application and environment
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();
            UUID applicationId = createApplication(adminToken, "Test App");

            var createResponse = post("/v1/applications/" + applicationId + "/environments",
                    new EnvironmentCreationRequest("Staging", "Staging env", PricingTier.BASIC),
                    adminToken, EnvironmentResponse.class);
            UUID environmentId = createResponse.getBody().id();

            // Create READ_ONLY user with app access
            String readOnlyToken =
                    createUserWithRoleAndAppAccess(adminToken, CustomerRole.READ_ONLY,
                            applicationId);

            // READ_ONLY should NOT be able to delete environment
            var response =
                    delete("/v1/applications/" + applicationId + "/environments/" + environmentId,
                            readOnlyToken, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void readOnlyRoleCanListEnvironments() {
            // Setup: Admin creates company and application
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();
            UUID applicationId = createApplication(adminToken, "Test App");

            // Create READ_ONLY user with app access
            String readOnlyToken =
                    createUserWithRoleAndAppAccess(adminToken, CustomerRole.READ_ONLY,
                            applicationId);

            // READ_ONLY should be able to list environments (read operation)
            var response = get("/v1/applications/" + applicationId + "/environments", readOnlyToken,
                    EnvironmentResponse[].class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSizeGreaterThanOrEqualTo(2); // Default environments
        }
    }
}
