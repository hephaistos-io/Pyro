package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.IntegrationTestSupport;
import io.hephaistos.flagforge.PostgresTestContainerConfiguration;
import io.hephaistos.flagforge.controller.dto.ApplicationCreationRequest;
import io.hephaistos.flagforge.controller.dto.ApplicationResponse;
import io.hephaistos.flagforge.data.PricingTier;
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

        var response =
                post("/v1/applications", new ApplicationCreationRequest("My Application"), token,
                        ApplicationResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("My Application");
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().pricingTier()).isEqualTo(PricingTier.FREE);
    }

    @Test
    void createSecondApplicationReturns201WithPaidPricingTier() {
        String token = registerAndAuthenticateWithCompany();

        // Create first application (should be FREE)
        var firstResponse =
                post("/v1/applications", new ApplicationCreationRequest("First App"), token,
                        ApplicationResponse.class);
        assertThat(firstResponse.getBody().pricingTier()).isEqualTo(PricingTier.FREE);

        // Create second application (should be PAID)
        var secondResponse =
                post("/v1/applications", new ApplicationCreationRequest("Second App"), token,
                        ApplicationResponse.class);

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(secondResponse.getBody()).isNotNull();
        assertThat(secondResponse.getBody().name()).isEqualTo("Second App");
        assertThat(secondResponse.getBody().pricingTier()).isEqualTo(PricingTier.PAID);
    }

    @Test
    void createThirdApplicationReturns201WithPaidPricingTier() {
        String token = registerAndAuthenticateWithCompany();

        // Create first application (should be FREE)
        post("/v1/applications", new ApplicationCreationRequest("First App"), token,
                ApplicationResponse.class);

        // Create second application (should be PAID)
        post("/v1/applications", new ApplicationCreationRequest("Second App"), token,
                ApplicationResponse.class);

        // Create third application (should also be PAID)
        var thirdResponse =
                post("/v1/applications", new ApplicationCreationRequest("Third App"), token,
                        ApplicationResponse.class);

        assertThat(thirdResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(thirdResponse.getBody().pricingTier()).isEqualTo(PricingTier.PAID);
    }

    @Test
    void createApplicationReturns404WhenUserHasNoCompany() {
        String token = registerAndAuthenticate();

        var response =
                post("/v1/applications", new ApplicationCreationRequest("My Application"), token,
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("MISSING_COMPANY_ASSIGNMENT");
    }

    @Test
    void createApplicationReturns409ForDuplicateName() {
        String token = registerAndAuthenticateWithCompany();
        var request = new ApplicationCreationRequest("Duplicate App");

        // Create first application
        post("/v1/applications", request, token, ApplicationResponse.class);

        // Try to create duplicate
        var response = post("/v1/applications", request, token, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("DUPLICATE_RESOURCE");
    }

    @Test
    void createApplicationReturns400ForTooShortName() {
        String token = registerAndAuthenticateWithCompany();

        var response =
                post("/v1/applications", new ApplicationCreationRequest("a"), token, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("VALIDATION_ERROR");
    }

    @Test
    void getApplicationsReturns200WithList() {
        String token = registerAndAuthenticateWithCompany();
        createApplication(token, "App 1");
        createApplication(token, "App 2");

        var response = get("/v1/applications", token, ApplicationResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getApplicationsReturnsEmptyListWhenNone() {
        String token = registerAndAuthenticateWithCompany();

        var response = get("/v1/applications", token, ApplicationResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getApplicationsReturns404WhenUserHasNoCompany() {
        String token = registerAndAuthenticate();

        var response = get("/v1/applications", token, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("MISSING_COMPANY_ASSIGNMENT");
    }

    private void createApplication(String token, String name) {
        post("/v1/applications", new ApplicationCreationRequest(name), token,
                ApplicationResponse.class);
    }
}
