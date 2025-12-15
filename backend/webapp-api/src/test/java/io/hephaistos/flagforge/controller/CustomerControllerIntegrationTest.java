package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.IntegrationTestSupport;
import io.hephaistos.flagforge.PostgresTestContainerConfiguration;
import io.hephaistos.flagforge.controller.dto.CustomerResponse;
import io.hephaistos.flagforge.data.CustomerEntity;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestContainerConfiguration.class)
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

        var allCustomersResponse = get("/v1/customer/all", token, CustomerResponse[].class);
        assertThat(allCustomersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(allCustomersResponse.getBody()).hasSize(1);
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

        var allCustomersResponse = get("/v1/customer/all", token, CustomerResponse[].class);
        assertThat(allCustomersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(allCustomersResponse.getBody()).hasSize(4);
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
        var responseA = get("/v1/customer/all", tokenA, CustomerResponse[].class);
        assertThat(responseA.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseA.getBody()).hasSize(3);
        for (CustomerResponse customer : responseA.getBody()) {
            assertThat(customer.companyId()).hasValue(companyA.id());
        }

        // User B should only see users from company B (themselves + 1 created)
        var responseB = get("/v1/customer/all", tokenB, CustomerResponse[].class);
        assertThat(responseB.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseB.getBody()).hasSize(2);
        for (CustomerResponse customer : responseB.getBody()) {
            assertThat(customer.companyId()).hasValue(companyB.id());
        }
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
