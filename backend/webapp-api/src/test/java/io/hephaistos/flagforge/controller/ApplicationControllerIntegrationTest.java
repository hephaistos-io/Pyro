package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.IntegrationTestSupport;
import io.hephaistos.flagforge.PostgresTestContainerConfiguration;
import io.hephaistos.flagforge.controller.dto.ApplicationCreationRequest;
import io.hephaistos.flagforge.controller.dto.ApplicationListResponse;
import io.hephaistos.flagforge.controller.dto.ApplicationResponse;
import io.hephaistos.flagforge.data.PricingTier;
import io.hephaistos.flagforge.data.TemplateType;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.data.repository.CompanyRepository;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestContainerConfiguration.class)
@Tag("integration")
class ApplicationControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @BeforeEach
    void beforeEach() {
        initializeTestSupport();
        applicationRepository.deleteAll();
        companyRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    void createFirstApplicationReturns201WithFreePricingTier() {
        String token = registerAndAuthenticateWithCompany();

        var createAppResponse =
                post("/v1/applications", new ApplicationCreationRequest("My Application"), token,
                        ApplicationResponse.class);

        assertThat(createAppResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createAppResponse.getBody()).isNotNull();
        assertThat(createAppResponse.getBody().name()).isEqualTo("My Application");
        assertThat(createAppResponse.getBody().id()).isNotNull();
        assertThat(createAppResponse.getBody().pricingTier()).isEqualTo(PricingTier.FREE);
    }

    @Test
    void createSecondApplicationReturns201WithPaidPricingTier() {
        String token = registerAndAuthenticateWithCompany();

        // Create first application (should be FREE)
        var firstAppResponse =
                post("/v1/applications", new ApplicationCreationRequest("First App"), token,
                        ApplicationResponse.class);
        assertThat(firstAppResponse.getBody().pricingTier()).isEqualTo(PricingTier.FREE);

        // Create second application (should be BASIC)
        var secondAppResponse =
                post("/v1/applications", new ApplicationCreationRequest("Second App"), token,
                        ApplicationResponse.class);

        assertThat(secondAppResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(secondAppResponse.getBody()).isNotNull();
        assertThat(secondAppResponse.getBody().name()).isEqualTo("Second App");
        assertThat(secondAppResponse.getBody().pricingTier()).isEqualTo(PricingTier.BASIC);
    }

    @Test
    void createThirdApplicationReturns201WithBasicPricingTier() {
        String token = registerAndAuthenticateWithCompany();

        // Create first application (should be FREE)
        post("/v1/applications", new ApplicationCreationRequest("First App"), token,
                ApplicationResponse.class);

        // Create second application (should be BASIC)
        post("/v1/applications", new ApplicationCreationRequest("Second App"), token,
                ApplicationResponse.class);

        // Create third application (should also be BASIC)
        var thirdAppResponse =
                post("/v1/applications", new ApplicationCreationRequest("Third App"), token,
                        ApplicationResponse.class);

        assertThat(thirdAppResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(thirdAppResponse.getBody().pricingTier()).isEqualTo(PricingTier.BASIC);
    }

    @Test
    void createApplicationReturns404WhenUserHasNoCompany() {
        String token = registerAndAuthenticate();

        var noCompanyResponse =
                post("/v1/applications", new ApplicationCreationRequest("My Application"), token,
                        String.class);

        assertThat(noCompanyResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(noCompanyResponse.getBody().isEmpty());
    }

    @Test
    void createApplicationReturns409ForDuplicateName() {
        String token = registerAndAuthenticateWithCompany();
        var request = new ApplicationCreationRequest("Duplicate App");

        // Create first application
        post("/v1/applications", request, token, ApplicationResponse.class);

        // Try to create duplicate
        var duplicateResponse = post("/v1/applications", request, token, String.class);

        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicateResponse.getBody()).contains("DUPLICATE_RESOURCE");
    }

    @Test
    void createApplicationReturns400ForTooShortName() {
        String token = registerAndAuthenticateWithCompany();

        var invalidNameResponse =
                post("/v1/applications", new ApplicationCreationRequest("a"), token, String.class);

        assertThat(invalidNameResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalidNameResponse.getBody()).contains("VALIDATION_ERROR");
    }

    @Test
    void getApplicationsReturns200WithList() {
        String token = registerAndAuthenticateWithCompany();
        createApplication(token, "App 1");
        createApplication(token, "App 2");

        var listApplicationsResponse =
                get("/v1/applications", token, ApplicationListResponse[].class);

        assertThat(listApplicationsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listApplicationsResponse.getBody()).hasSize(2);
    }

    @Test
    void getApplicationsReturnsEmptyListWhenNone() {
        String token = registerAndAuthenticateWithCompany();

        var emptyListResponse = get("/v1/applications", token, ApplicationListResponse[].class);

        assertThat(emptyListResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(emptyListResponse.getBody()).isEmpty();
    }

    @Test
    void getApplicationsReturnsEmptyListWhenUserHasNoCompany() {
        String token = registerAndAuthenticate();

        var noCompanyResponse = get("/v1/applications", token, String.class);

        assertThat(noCompanyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(noCompanyResponse.getBody()).isEqualTo("[]");
    }

    @Test
    void getApplicationsReturnsOnlyAccessibleApplications() {
        // Create user with app access, with company and applications
        registerUser("User", "WithAccess", "user-with-access@example.com", "password123");
        String tokenUserWithAccess = authenticate("user-with-access@example.com", "password123");
        createCompany(tokenUserWithAccess);
        tokenUserWithAccess = authenticate("user-with-access@example.com", "password123");
        createApplication(tokenUserWithAccess, "App 1");
        createApplication(tokenUserWithAccess, "App 2");

        // Create user without app access and manually assign to same company
        registerUser("User", "WithoutAccess", "user-without-access@example.com", "password123");
        var customerWithAccess =
                customerRepository.findByEmail("user-with-access@example.com").orElseThrow();
        var customerWithoutAccess =
                customerRepository.findByEmail("user-without-access@example.com").orElseThrow();
        customerWithoutAccess.setCompanyId(customerWithAccess.getCompanyId().orElseThrow());
        customerRepository.saveAndFlush(customerWithoutAccess);

        // Re-authenticate user without access to get token with companyId
        String tokenUserWithoutAccess =
                authenticate("user-without-access@example.com", "password123");

        // User without access is in the same company but has no application access, so should see empty list
        var noAccessUserResponse =
                get("/v1/applications", tokenUserWithoutAccess, ApplicationListResponse[].class);
        assertThat(noAccessUserResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(noAccessUserResponse.getBody()).isEmpty();

        // User with access should still see both applications
        var accessUserResponse =
                get("/v1/applications", tokenUserWithAccess, ApplicationListResponse[].class);
        assertThat(accessUserResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(accessUserResponse.getBody()).hasSize(2);
    }

    @Test
    void applicationsAreIsolatedBetweenCompanies() {
        // Create user in company A with applications
        registerUser("User", "CompanyA", "user-company-a@example.com", "password123");
        String tokenCompanyA = authenticate("user-company-a@example.com", "password123");
        createCompany(tokenCompanyA, "Company A");
        tokenCompanyA = authenticate("user-company-a@example.com", "password123");
        createApplication(tokenCompanyA, "App A1");
        createApplication(tokenCompanyA, "App A2");

        // Create user in company B with different applications
        registerUser("User", "CompanyB", "user-company-b@example.com", "password123");
        String tokenCompanyB = authenticate("user-company-b@example.com", "password123");
        createCompany(tokenCompanyB, "Company B");
        tokenCompanyB = authenticate("user-company-b@example.com", "password123");
        createApplication(tokenCompanyB, "App B1");

        // User in company A should only see Company A's apps
        var companyAUserResponse =
                get("/v1/applications", tokenCompanyA, ApplicationListResponse[].class);
        assertThat(companyAUserResponse.getBody()).hasSize(2);
        assertThat(companyAUserResponse.getBody()).extracting("name")
                .containsExactlyInAnyOrder("App A1", "App A2");

        // User in company B should only see Company B's apps
        var companyBUserResponse =
                get("/v1/applications", tokenCompanyB, ApplicationListResponse[].class);
        assertThat(companyBUserResponse.getBody()).hasSize(1);
        assertThat(companyBUserResponse.getBody()[0].name()).isEqualTo("App B1");
    }

    @Test
    void newlyCreatedApplicationIsImmediatelyVisible() {
        String token = registerAndAuthenticateWithCompany();

        // Create an application
        var createAppResponse =
                post("/v1/applications", new ApplicationCreationRequest("New App"), token,
                        ApplicationResponse.class);
        assertThat(createAppResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // The application should be immediately visible without re-authentication
        var listAfterCreateResponse =
                get("/v1/applications", token, ApplicationListResponse[].class);
        assertThat(listAfterCreateResponse.getBody()).hasSize(1);
        assertThat(listAfterCreateResponse.getBody()[0].name()).isEqualTo("New App");
    }

    // ========== Template Auto-Creation Tests ==========

    @Test
    void createApplicationReturnsWithBothTemplates() {
        String token = registerAndAuthenticateWithCompany();

        var response = post("/v1/applications", new ApplicationCreationRequest("My App"), token,
                ApplicationResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().templates()).hasSize(2);
        assertThat(response.getBody().templates()).extracting("type")
                .containsExactlyInAnyOrder(TemplateType.USER, TemplateType.SYSTEM);
    }

    @Test
    void autoCreatedTemplatesHaveEmptySchema() {
        String token = registerAndAuthenticateWithCompany();

        var response = post("/v1/applications", new ApplicationCreationRequest("My App"), token,
                ApplicationResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        for (var template : response.getBody().templates()) {
            assertThat(template.schema().fields()).isEmpty();
        }
    }

    @Test
    void revokedApplicationAccessIsReflectedWithoutReAuthentication() {
        // Setup: Create user with company and two applications
        String token = registerAndAuthenticateWithCompany();
        var createApp1Response =
                post("/v1/applications", new ApplicationCreationRequest("App 1"), token,
                        ApplicationResponse.class);
        var createApp2Response =
                post("/v1/applications", new ApplicationCreationRequest("App 2"), token,
                        ApplicationResponse.class);

        // Verify user can see both applications
        var beforeRevokeResponse = get("/v1/applications", token, ApplicationListResponse[].class);
        assertThat(beforeRevokeResponse.getBody()).hasSize(2);

        // Revoke access to App 2 directly in the database (without re-authentication)
        var customer = customerRepository.findByEmailWithAccessibleApplications("test@example.com")
                .orElseThrow();
        var app2Entity =
                applicationRepository.findById(createApp2Response.getBody().id()).orElseThrow();
        customer.getAccessibleApplications().remove(app2Entity);
        customerRepository.saveAndFlush(customer);

        // Verify the database was actually updated
        var updatedCustomer =
                customerRepository.findByEmailWithAccessibleApplications("test@example.com")
                        .orElseThrow();
        assertThat(updatedCustomer.getAccessibleApplications()).hasSize(1);

        // Without re-authentication, user should only see App 1
        var afterRevokeResponse = get("/v1/applications", token, ApplicationListResponse[].class);
        assertThat(afterRevokeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(afterRevokeResponse.getBody()).hasSize(1);
        assertThat(afterRevokeResponse.getBody()[0].name()).isEqualTo("App 1");
        assertThat(afterRevokeResponse.getBody()[0].id()).isEqualTo(
                createApp1Response.getBody().id());
    }

    private void createApplication(String token, String name) {
        post("/v1/applications", new ApplicationCreationRequest(name), token,
                ApplicationResponse.class);
    }
}
