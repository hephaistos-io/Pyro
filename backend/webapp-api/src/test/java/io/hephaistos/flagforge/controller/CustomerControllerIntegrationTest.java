package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.IntegrationTestSupport;
import io.hephaistos.flagforge.MailpitTestConfiguration;
import io.hephaistos.flagforge.PostgresTestContainerConfiguration;
import io.hephaistos.flagforge.RedisTestContainerConfiguration;
import io.hephaistos.flagforge.common.data.ApplicationEntity;
import io.hephaistos.flagforge.common.data.CustomerEntity;
import io.hephaistos.flagforge.common.enums.CustomerRole;
import io.hephaistos.flagforge.controller.dto.ApplicationCreationRequest;
import io.hephaistos.flagforge.controller.dto.ApplicationResponse;
import io.hephaistos.flagforge.controller.dto.CustomerResponse;
import io.hephaistos.flagforge.controller.dto.TeamResponse;
import io.hephaistos.flagforge.controller.dto.UpdateCustomerRequest;
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

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestContainerConfiguration.class, RedisTestContainerConfiguration.class,
        MailpitTestConfiguration.class})
@Tag("integration")
class CustomerControllerIntegrationTest extends IntegrationTestSupport {

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
    void noCompanyAssignedReturnsError() {
        //create several users with no company
        registerUserWithRandomEmail();
        registerUserWithRandomEmail();
        registerUserWithRandomEmail();
        String token = registerAndAuthenticate();

        var allCustomersResponse = get("/v1/customer/all", token, String.class);
        assertThat(allCustomersResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(allCustomersResponse.getBody()).contains("MISSING_COMPANY_ASSIGNMENT");
    }

    @Test
    void companyWithOnlyOneUserReturnsOneUser() {
        String token = registerAndAuthenticateWithCompany();

        createCompany(token, "TestCompanyPleaseIgnore");

        var response = get("/v1/customer/all", token, TeamResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().members()).hasSize(1);
    }

    @Test
    void companyWithMultipleUsersReturnsAllOfThem() {
        // Get company ID from the customer in the database after creating it
        String token = registerAndAuthenticateWithCompany();

        // Get the company ID from the current user's profile
        var profile = get("/v1/customer/profile", token, CustomerResponse.class);
        assertThat(profile.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(profile.getBody().companyId()).isPresent();
        UUID companyId = profile.getBody().companyId().orElseThrow();

        createUserForCompany(companyId);
        createUserForCompany(companyId);
        createUserForCompany(companyId);

        var response = get("/v1/customer/all", token, TeamResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().members()).hasSize(4);
    }

    @Test
    void usersFromDifferentCompaniesAreIsolated() {
        // Create first user with company A
        registerUser("UserA", "LastA", "usera@test.com", "password123");
        String tokenA = authenticate("usera@test.com", "password123");
        var companyA = createCompany(tokenA, "Company A");
        assertThat(companyA).as("Company A creation should succeed").isNotNull();
        createUserForCompany(companyA.id());
        createUserForCompany(companyA.id());

        // Create second user with company B
        registerUser("UserB", "LastB", "userb@test.com", "password123");
        String tokenB = authenticate("userb@test.com", "password123");
        var companyB = createCompany(tokenB, "Company B");
        assertThat(companyB).as("Company B creation should succeed").isNotNull();
        createUserForCompany(companyB.id());

        // User A should only see users from company A (themselves + 2 created)
        var responseA = get("/v1/customer/all", tokenA, TeamResponse.class);
        assertThat(responseA.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseA.getBody().members()).hasSize(3);
        for (CustomerResponse customer : responseA.getBody().members()) {
            assertThat(customer.companyId()).hasValue(companyA.id());
        }

        // User B should only see users from company B (themselves + 1 created)
        var responseB = get("/v1/customer/all", tokenB, TeamResponse.class);
        assertThat(responseB.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseB.getBody().members()).hasSize(2);
        for (CustomerResponse customer : responseB.getBody().members()) {
            assertThat(customer.companyId()).hasValue(companyB.id());
        }
    }

    @Test
    void updateCustomerApplicationAccessPersistsChanges() {
        // Setup: Create user with company and applications
        String token = registerAndAuthenticateWithCompany();
        var profile = get("/v1/customer/profile", token, CustomerResponse.class);
        UUID companyId = profile.getBody().companyId().orElseThrow();

        // Create applications
        var app1 = createApplication(token, "App1");
        var app2 = createApplication(token, "App2");
        var app3 = createApplication(token, "App3");

        // Create a developer user with access to app1
        CustomerEntity devUser = new CustomerEntity();
        devUser.setFirstName("Dev");
        devUser.setLastName("User");
        devUser.setEmail(randomEmail());
        devUser.setPassword(UUID.randomUUID().toString());
        devUser.setCompanyId(companyId);
        devUser.setRole(CustomerRole.DEV);

        ApplicationEntity app1Entity = applicationRepository.findById(app1.id()).orElseThrow();
        devUser.setAccessibleApplications(new HashSet<>(List.of(app1Entity)));
        devUser = customerRepository.save(devUser);

        // Verify initial state
        var initialUser = customerRepository.findByIdFiltered(devUser.getId()).orElseThrow();
        assertThat(initialUser.getAccessibleApplications()).hasSize(1);
        assertThat(initialUser.getAccessibleApplications()).extracting(ApplicationEntity::getId)
                .containsExactly(app1.id());

        // Update: Change access from app1 to app2 and app3
        var updateRequest =
                new UpdateCustomerRequest(List.of(app2.id(), app3.id()), CustomerRole.DEV);
        var updateResponse = put("/v1/customer/" + devUser.getId(), updateRequest, token,
                CustomerResponse.class);

        // Assert: Response is successful
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().id()).isEqualTo(devUser.getId());
        assertThat(updateResponse.getBody().applications()).hasSize(2);
        assertThat(updateResponse.getBody().applications()).extracting(app -> app.id())
                .containsExactlyInAnyOrder(app2.id(), app3.id());

        // Assert: Changes are persisted in database
        var updatedUser = customerRepository.findByIdFiltered(devUser.getId()).orElseThrow();
        assertThat(updatedUser.getAccessibleApplications()).hasSize(2);
        assertThat(updatedUser.getAccessibleApplications()).extracting(ApplicationEntity::getId)
                .containsExactlyInAnyOrder(app2.id(), app3.id());
    }

    private ApplicationResponse createApplication(String token, String name) {
        var request = new ApplicationCreationRequest(name);
        var response = post("/v1/applications", request, token, ApplicationResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private void createUserForCompany(UUID id) {
        CustomerEntity customerEntity = new CustomerEntity();
        customerEntity.setFirstName("First");
        customerEntity.setLastName("Last");
        customerEntity.setEmail(randomEmail());
        customerEntity.setPassword(UUID.randomUUID().toString());
        customerEntity.setCompanyId(id);
        customerRepository.save(customerEntity);
    }
}
