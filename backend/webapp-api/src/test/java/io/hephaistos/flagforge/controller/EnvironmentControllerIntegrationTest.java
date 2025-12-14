package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.IntegrationTestSupport;
import io.hephaistos.flagforge.PostgresTestContainerConfiguration;
import io.hephaistos.flagforge.controller.dto.ApplicationCreationRequest;
import io.hephaistos.flagforge.controller.dto.ApplicationResponse;
import io.hephaistos.flagforge.controller.dto.EnvironmentCreationRequest;
import io.hephaistos.flagforge.controller.dto.EnvironmentResponse;
import io.hephaistos.flagforge.data.PricingTier;
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
                new EnvironmentCreationRequest("Production", "Production environment"), token,
                EnvironmentResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Production");
        assertThat(response.getBody().description()).isEqualTo("Production environment");
        assertThat(response.getBody().tier()).isEqualTo(PricingTier.PAID);
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
                new EnvironmentCreationRequest("Production", "Prod env"), token,
                EnvironmentResponse.class);

        var response = get("/v1/applications/" + applicationId + "/environments", token,
                EnvironmentResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2); // Default + Production
    }

    @Test
    void deleteEnvironmentReturns204ForPaidTier() {
        String token = registerAndAuthenticateWithCompany();
        UUID applicationId = createApplication(token, "Test App");

        // Create a PAID tier environment
        var createResponse = post("/v1/applications/" + applicationId + "/environments",
                new EnvironmentCreationRequest("Production", "Prod env"), token,
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

        assertThat(listResponse.getBody()).hasSize(1); // Only default environment remains
        assertThat(listResponse.getBody()[0].tier()).isEqualTo(PricingTier.FREE);
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
                new EnvironmentCreationRequest("Production", "Prod"), token,
                EnvironmentResponse.class);
        UUID environmentId = createResponse.getBody().id();

        // Try to delete via app 2
        var deleteResponse =
                delete("/v1/applications/" + applicationId2 + "/environments/" + environmentId,
                        token, String.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private UUID createApplication(String token, String name) {
        var response = post("/v1/applications", new ApplicationCreationRequest(name), token,
                ApplicationResponse.class);
        return response.getBody().id();
    }
}
