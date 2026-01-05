package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.IntegrationTestSupport;
import io.hephaistos.flagforge.MailpitTestConfiguration;
import io.hephaistos.flagforge.PostgresTestContainerConfiguration;
import io.hephaistos.flagforge.RedisTestContainerConfiguration;
import io.hephaistos.flagforge.common.enums.CustomerRole;
import io.hephaistos.flagforge.common.enums.TemplateType;
import io.hephaistos.flagforge.common.types.BooleanTemplateField;
import io.hephaistos.flagforge.common.types.TemplateSchema;
import io.hephaistos.flagforge.controller.dto.AllTemplateOverridesResponse;
import io.hephaistos.flagforge.controller.dto.ApplicationCreationRequest;
import io.hephaistos.flagforge.controller.dto.ApplicationResponse;
import io.hephaistos.flagforge.controller.dto.EnvironmentResponse;
import io.hephaistos.flagforge.controller.dto.InviteCreationRequest;
import io.hephaistos.flagforge.controller.dto.InviteCreationResponse;
import io.hephaistos.flagforge.controller.dto.MergedTemplateValuesResponse;
import io.hephaistos.flagforge.controller.dto.TemplateResponse;
import io.hephaistos.flagforge.controller.dto.TemplateUpdateRequest;
import io.hephaistos.flagforge.controller.dto.TemplateValuesRequest;
import io.hephaistos.flagforge.controller.dto.TemplateValuesResponse;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.data.repository.CompanyInviteRepository;
import io.hephaistos.flagforge.data.repository.CompanyRepository;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.data.repository.EnvironmentRepository;
import io.hephaistos.flagforge.data.repository.TemplateRepository;
import io.hephaistos.flagforge.data.repository.TemplateValuesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestContainerConfiguration.class, RedisTestContainerConfiguration.class,
        MailpitTestConfiguration.class})
@Tag("integration")
class TemplateControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateValuesRepository templateValuesRepository;

    @Autowired
    private CompanyInviteRepository companyInviteRepository;

    @BeforeEach
    void beforeEach() {
        initializeTestSupport();
        companyInviteRepository.deleteAll();
        templateValuesRepository.deleteAll();
        templateRepository.deleteAll();
        environmentRepository.deleteAll();
        applicationRepository.deleteAll();
        companyRepository.deleteAll();
        customerRepository.deleteAll();
    }

    // ========== Auto-creation Tests ==========

    @Test
    void templatesAreAutoCreatedWhenApplicationIsCreated() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");

        var response = get("/v1/applications/" + applicationId + "/templates", token,
                TemplateResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).extracting(TemplateResponse::type)
                .containsExactlyInAnyOrder(TemplateType.USER, TemplateType.SYSTEM);
    }

    // ========== Template CRUD Tests ==========

    @Test
    void getTemplatesReturnsBothTemplateTypes() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");

        var response = get("/v1/applications/" + applicationId + "/templates", token,
                TemplateResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void listAllOverridesReturnsEmptyWhenNoOverrides() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");
        UUID environmentId = getDefaultEnvironmentId(token, applicationId);

        var response =
                get("/v1/applications/" + applicationId + "/templates/overrides?environmentId=" + environmentId,
                        token, AllTemplateOverridesResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().userOverrides()).isEmpty();
        assertThat(response.getBody().systemOverrides()).isEmpty();
    }

    @Test
    void listAllOverridesReturnsOverridesGroupedByType() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");
        UUID environmentId = getDefaultEnvironmentId(token, applicationId);

        // Update templates to have fields
        put("/v1/applications/" + applicationId + "/templates/USER",
                new TemplateUpdateRequest(createTestSchema()), token, TemplateResponse.class);
        put("/v1/applications/" + applicationId + "/templates/SYSTEM",
                new TemplateUpdateRequest(createTestSchema()), token, TemplateResponse.class);

        // Create USER overrides
        put("/v1/applications/" + applicationId + "/templates/USER/environments/" + environmentId + "/overrides/user-1",
                new TemplateValuesRequest(Map.of("night_mode", true)), token,
                TemplateValuesResponse.class);
        put("/v1/applications/" + applicationId + "/templates/USER/environments/" + environmentId + "/overrides/user-2",
                new TemplateValuesRequest(Map.of("night_mode", false)), token,
                TemplateValuesResponse.class);

        // Create SYSTEM override
        put("/v1/applications/" + applicationId + "/templates/SYSTEM/environments/" + environmentId + "/overrides/sys-1",
                new TemplateValuesRequest(Map.of("night_mode", true)), token,
                TemplateValuesResponse.class);

        var response =
                get("/v1/applications/" + applicationId + "/templates/overrides?environmentId=" + environmentId,
                        token, AllTemplateOverridesResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().userOverrides()).hasSize(2);
        assertThat(response.getBody().systemOverrides()).hasSize(1);
        assertThat(response.getBody().userOverrides()).extracting("identifier")
                .containsExactlyInAnyOrder("user-1", "user-2");
        assertThat(response.getBody().systemOverrides()).extracting("identifier")
                .containsExactly("sys-1");
    }

    // ========== Multi-tenancy Tests ==========

    @Test
    void templatesAreIsolatedBetweenCompanies() {
        // User 1 with Company A
        registerUser("User", "One", "user1@example.com", "password123");
        String token1 = authenticate("user1@example.com", "password123");
        createCompany(token1, "Company A");
        token1 = authenticate("user1@example.com", "password123");
        UUID appIdA = createApplication(token1, "App A");

        // User 2 with Company B
        registerUser("User", "Two", "user2@example.com", "password123");
        String token2 = authenticate("user2@example.com", "password123");
        createCompany(token2, "Company B");
        token2 = authenticate("user2@example.com", "password123");

        // User 2 should NOT be able to access Company A's templates
        var response = get("/v1/applications/" + appIdA + "/templates", token2, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ========== Per-Type Parameterized Tests ==========


    @Nested
    @ParameterizedClass
    @EnumSource(TemplateType.class)
    class PerTypeTests {

        @Parameter
        TemplateType type;

        @Test
        void autoCreatedTemplateHasEmptySchema() {
            String token = registerAndAuthenticateWithCompany();
            UUID applicationId = createApplication(token, "Test App");

            var response =
                    get("/v1/applications/" + applicationId + "/templates?type=" + type, token,
                            TemplateResponse[].class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody()[0].schema().fields()).isEmpty();
        }

        @Test
        void getTemplateByTypeReturnsSuccessfully() {
            String token = registerAndAuthenticateWithCompany();
            UUID applicationId = createApplication(token, "Test App");

            var response =
                    get("/v1/applications/" + applicationId + "/templates?type=" + type, token,
                            TemplateResponse[].class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody()[0].type()).isEqualTo(type);
        }

        @Test
        void updateTemplateSuccessfully() {
            String token = registerAndAuthenticateWithCompany();
            UUID applicationId = createApplication(token, "Test App");

            var newSchema = createTestSchema();

            var response = put("/v1/applications/" + applicationId + "/templates/" + type,
                    new TemplateUpdateRequest(newSchema), token, TemplateResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().schema().fields()).hasSize(1);
            assertThat(response.getBody().schema().fields().getFirst().key()).isEqualTo(
                    "night_mode");
        }

        @Test
        void getMergedValuesReturnsDefaultsFromSchema() {
            String token = registerAndAuthenticateWithCompany();
            UUID applicationId = createApplication(token, "Test App");
            UUID environmentId = getDefaultEnvironmentId(token, applicationId);

            // Update template to have a field with default value
            put("/v1/applications/" + applicationId + "/templates/" + type,
                    new TemplateUpdateRequest(createTestSchema()), token, TemplateResponse.class);

            var response =
                    get("/v1/applications/" + applicationId + "/templates/" + type + "/environments/" + environmentId + "/values",
                            token, MergedTemplateValuesResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().values()).containsEntry("night_mode", false);
            assertThat(response.getBody().appliedIdentifiers()).isEmpty();
        }

        @Test
        void setOverrideReturns200() {
            String token = registerAndAuthenticateWithCompany();
            UUID applicationId = createApplication(token, "Test App");
            UUID environmentId = getDefaultEnvironmentId(token, applicationId);

            // Update template to have a field
            put("/v1/applications/" + applicationId + "/templates/" + type,
                    new TemplateUpdateRequest(createTestSchema()), token, TemplateResponse.class);

            var response =
                    put("/v1/applications/" + applicationId + "/templates/" + type + "/environments/" + environmentId + "/overrides/id-123",
                            new TemplateValuesRequest(Map.of("night_mode", true)), token,
                            TemplateValuesResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().identifier()).isEqualTo("id-123");
            assertThat(response.getBody().values()).containsEntry("night_mode", true);
        }

        @Test
        void getMergedValuesAppliesOverrides() {
            String token = registerAndAuthenticateWithCompany();
            UUID applicationId = createApplication(token, "Test App");
            UUID environmentId = getDefaultEnvironmentId(token, applicationId);

            // Update template to have a field
            put("/v1/applications/" + applicationId + "/templates/" + type,
                    new TemplateUpdateRequest(createTestSchema()), token, TemplateResponse.class);

            put("/v1/applications/" + applicationId + "/templates/" + type + "/environments/" + environmentId + "/overrides/id-123",
                    new TemplateValuesRequest(Map.of("night_mode", true)), token,
                    TemplateValuesResponse.class);

            var response =
                    get("/v1/applications/" + applicationId + "/templates/" + type + "/environments/" + environmentId + "/values?identifiers=id-123",
                            token, MergedTemplateValuesResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().values()).containsEntry("night_mode", true);
            assertThat(response.getBody().appliedIdentifiers()).containsExactly("id-123");
        }

        @Test
        void getMergedValuesAppliesMultipleOverridesInOrder() {
            String token = registerAndAuthenticateWithCompany();
            UUID applicationId = createApplication(token, "Test App");
            UUID environmentId = getDefaultEnvironmentId(token, applicationId);

            // Update template to have a field
            put("/v1/applications/" + applicationId + "/templates/" + type,
                    new TemplateUpdateRequest(createTestSchema()), token, TemplateResponse.class);

            // Create two overrides
            put("/v1/applications/" + applicationId + "/templates/" + type + "/environments/" + environmentId + "/overrides/region-eu",
                    new TemplateValuesRequest(Map.of("night_mode", true)), token,
                    TemplateValuesResponse.class);
            put("/v1/applications/" + applicationId + "/templates/" + type + "/environments/" + environmentId + "/overrides/id-123",
                    new TemplateValuesRequest(Map.of("night_mode", false)), token,
                    TemplateValuesResponse.class);

            // Request with id-123 last (should win)
            var response =
                    get("/v1/applications/" + applicationId + "/templates/" + type + "/environments/" + environmentId + "/values?identifiers=region-eu,id-123",
                            token, MergedTemplateValuesResponse.class);

            assertThat(response.getBody().values()).containsEntry("night_mode", false);
            assertThat(response.getBody().appliedIdentifiers()).containsExactly("region-eu",
                    "id-123");
        }

        @Test
        void listOverridesReturnsAllOverrides() {
            String token = registerAndAuthenticateWithCompany();
            UUID applicationId = createApplication(token, "Test App");
            UUID environmentId = getDefaultEnvironmentId(token, applicationId);

            // Update template to have a field
            put("/v1/applications/" + applicationId + "/templates/" + type,
                    new TemplateUpdateRequest(createTestSchema()), token, TemplateResponse.class);

            put("/v1/applications/" + applicationId + "/templates/" + type + "/environments/" + environmentId + "/overrides/id-1",
                    new TemplateValuesRequest(Map.of("night_mode", true)), token,
                    TemplateValuesResponse.class);
            put("/v1/applications/" + applicationId + "/templates/" + type + "/environments/" + environmentId + "/overrides/id-2",
                    new TemplateValuesRequest(Map.of("night_mode", false)), token,
                    TemplateValuesResponse.class);

            var response =
                    get("/v1/applications/" + applicationId + "/templates/" + type + "/environments/" + environmentId + "/overrides",
                            token, TemplateValuesResponse[].class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
        }

        @Test
        void deleteOverrideReturns204() {
            String token = registerAndAuthenticateWithCompany();
            UUID applicationId = createApplication(token, "Test App");
            UUID environmentId = getDefaultEnvironmentId(token, applicationId);

            // Update template to have a field
            put("/v1/applications/" + applicationId + "/templates/" + type,
                    new TemplateUpdateRequest(createTestSchema()), token, TemplateResponse.class);

            put("/v1/applications/" + applicationId + "/templates/" + type + "/environments/" + environmentId + "/overrides/id-123",
                    new TemplateValuesRequest(Map.of("night_mode", true)), token,
                    TemplateValuesResponse.class);

            var response =
                    delete("/v1/applications/" + applicationId + "/templates/" + type + "/environments/" + environmentId + "/overrides/id-123",
                            token, Void.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            // Verify deletion
            var listResponse =
                    get("/v1/applications/" + applicationId + "/templates/" + type + "/environments/" + environmentId + "/overrides",
                            token, TemplateValuesResponse[].class);
            assertThat(listResponse.getBody()).isEmpty();
        }
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

    private TemplateSchema createTestSchema() {
        return new TemplateSchema(
                List.of(new BooleanTemplateField("night_mode", "Dark theme preference", true,
                        false)));
    }

    private String createUserWithRoleAndAppAccess(String adminToken, CustomerRole role,
            UUID applicationId) {
        String email = UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        var inviteRequest = new InviteCreationRequest(email, role, Set.of(applicationId), null);
        post("/v1/company/invite", inviteRequest, adminToken, InviteCreationResponse.class);

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
        void devRoleCanUpdateTemplate() {
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();
            UUID applicationId = createApplication(adminToken, "Test App");

            String devToken =
                    createUserWithRoleAndAppAccess(adminToken, CustomerRole.DEV, applicationId);

            var response = put("/v1/applications/" + applicationId + "/templates/USER",
                    new TemplateUpdateRequest(createTestSchema()), devToken,
                    TemplateResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void readOnlyRoleCannotUpdateTemplate() {
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();
            UUID applicationId = createApplication(adminToken, "Test App");

            String readOnlyToken =
                    createUserWithRoleAndAppAccess(adminToken, CustomerRole.READ_ONLY,
                            applicationId);

            var response = put("/v1/applications/" + applicationId + "/templates/USER",
                    new TemplateUpdateRequest(createTestSchema()), readOnlyToken, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void devRoleCanSetOverride() {
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();
            UUID applicationId = createApplication(adminToken, "Test App");
            UUID environmentId = getDefaultEnvironmentId(adminToken, applicationId);

            // First update template schema
            put("/v1/applications/" + applicationId + "/templates/USER",
                    new TemplateUpdateRequest(createTestSchema()), adminToken,
                    TemplateResponse.class);

            String devToken =
                    createUserWithRoleAndAppAccess(adminToken, CustomerRole.DEV, applicationId);

            var response =
                    put("/v1/applications/" + applicationId + "/templates/USER/environments/" + environmentId + "/overrides/id-123",
                            new TemplateValuesRequest(Map.of("night_mode", true)), devToken,
                            TemplateValuesResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void readOnlyRoleCannotSetOverride() {
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();
            UUID applicationId = createApplication(adminToken, "Test App");
            UUID environmentId = getDefaultEnvironmentId(adminToken, applicationId);

            // First update template schema
            put("/v1/applications/" + applicationId + "/templates/USER",
                    new TemplateUpdateRequest(createTestSchema()), adminToken,
                    TemplateResponse.class);

            String readOnlyToken =
                    createUserWithRoleAndAppAccess(adminToken, CustomerRole.READ_ONLY,
                            applicationId);

            var response =
                    put("/v1/applications/" + applicationId + "/templates/USER/environments/" + environmentId + "/overrides/id-123",
                            new TemplateValuesRequest(Map.of("night_mode", true)), readOnlyToken,
                            String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void devRoleCanDeleteOverride() {
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();
            UUID applicationId = createApplication(adminToken, "Test App");
            UUID environmentId = getDefaultEnvironmentId(adminToken, applicationId);

            // Setup: update template and create override
            put("/v1/applications/" + applicationId + "/templates/USER",
                    new TemplateUpdateRequest(createTestSchema()), adminToken,
                    TemplateResponse.class);
            put("/v1/applications/" + applicationId + "/templates/USER/environments/" + environmentId + "/overrides/id-123",
                    new TemplateValuesRequest(Map.of("night_mode", true)), adminToken,
                    TemplateValuesResponse.class);

            String devToken =
                    createUserWithRoleAndAppAccess(adminToken, CustomerRole.DEV, applicationId);

            var response =
                    delete("/v1/applications/" + applicationId + "/templates/USER/environments/" + environmentId + "/overrides/id-123",
                            devToken, Void.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        void readOnlyRoleCannotDeleteOverride() {
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();
            UUID applicationId = createApplication(adminToken, "Test App");
            UUID environmentId = getDefaultEnvironmentId(adminToken, applicationId);

            // Setup: update template and create override
            put("/v1/applications/" + applicationId + "/templates/USER",
                    new TemplateUpdateRequest(createTestSchema()), adminToken,
                    TemplateResponse.class);
            put("/v1/applications/" + applicationId + "/templates/USER/environments/" + environmentId + "/overrides/id-123",
                    new TemplateValuesRequest(Map.of("night_mode", true)), adminToken,
                    TemplateValuesResponse.class);

            String readOnlyToken =
                    createUserWithRoleAndAppAccess(adminToken, CustomerRole.READ_ONLY,
                            applicationId);

            var response =
                    delete("/v1/applications/" + applicationId + "/templates/USER/environments/" + environmentId + "/overrides/id-123",
                            readOnlyToken, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void readOnlyRoleCanListTemplates() {
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();
            UUID applicationId = createApplication(adminToken, "Test App");

            String readOnlyToken =
                    createUserWithRoleAndAppAccess(adminToken, CustomerRole.READ_ONLY,
                            applicationId);

            // Read operations should be allowed
            var response = get("/v1/applications/" + applicationId + "/templates", readOnlyToken,
                    TemplateResponse[].class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
        }

        @Test
        void readOnlyRoleCanGetMergedValues() {
            String adminToken = registerAndAuthenticateWithCompany();
            adminToken = authenticate();
            UUID applicationId = createApplication(adminToken, "Test App");
            UUID environmentId = getDefaultEnvironmentId(adminToken, applicationId);

            // Setup: update template
            put("/v1/applications/" + applicationId + "/templates/USER",
                    new TemplateUpdateRequest(createTestSchema()), adminToken,
                    TemplateResponse.class);

            String readOnlyToken =
                    createUserWithRoleAndAppAccess(adminToken, CustomerRole.READ_ONLY,
                            applicationId);

            var response =
                    get("/v1/applications/" + applicationId + "/templates/USER/environments/" + environmentId + "/values",
                            readOnlyToken, MergedTemplateValuesResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}
