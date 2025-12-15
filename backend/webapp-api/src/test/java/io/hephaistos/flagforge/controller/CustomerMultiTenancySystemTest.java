package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.IntegrationTestSupport;
import io.hephaistos.flagforge.PostgresTestContainerConfiguration;
import io.hephaistos.flagforge.controller.dto.CustomerResponse;
import io.hephaistos.flagforge.data.CustomerEntity;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.data.repository.CompanyRepository;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * System test for customer multi-tenancy isolation. Verifies that the /v1/customer/all endpoint
 * correctly filters customers by company.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestContainerConfiguration.class)
@Tag("integration")
@Tag("system")
class CustomerMultiTenancySystemTest extends IntegrationTestSupport {

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
    @DisplayName("Customer isolation: users only see customers from their own company")
    void customerIsolationWithMultipleCompaniesAndUnassignedUser() {
        // Setup: Create 1 user with no company, 1 user with company A, 1 user with company B

        // User with no company
        registerUser("NoCompany", "User", "nocompany@test.com", "password123");
        String tokenNoCompany = authenticate("nocompany@test.com", "password123");

        // User with Company A (plus additional users in company A)
        registerUser("UserA", "CompanyA", "usera@test.com", "password123");
        String tokenA = authenticate("usera@test.com", "password123");
        var companyA = createCompany(tokenA, "Company A");
        assertThat(companyA).as("Company A should be created").isNotNull();
        // Add more users to company A directly in database
        createUserForCompany(companyA.id(), "ExtraA1", "extra1a@test.com");
        createUserForCompany(companyA.id(), "ExtraA2", "extra2a@test.com");

        // User with Company B (plus additional users in company B)
        registerUser("UserB", "CompanyB", "userb@test.com", "password123");
        String tokenB = authenticate("userb@test.com", "password123");
        var companyB = createCompany(tokenB, "Company B");
        assertThat(companyB).as("Company B should be created").isNotNull();
        // Add more users to company B directly in database
        createUserForCompany(companyB.id(), "ExtraB1", "extra1b@test.com");

        // Verify: User with no company cannot access customer list
        var responseNoCompany = get("/v1/customer/all", tokenNoCompany, String.class);
        assertThat(responseNoCompany.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseNoCompany.getBody()).contains("MISSING_COMPANY_ASSIGNMENT");

        // Verify: User A sees only Company A customers (themselves + 2 extra)
        var responseA = get("/v1/customer/all", tokenA, CustomerResponse[].class);
        assertThat(responseA.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseA.getBody()).as("User A should see exactly 3 customers from Company A")
                .hasSize(3);
        for (CustomerResponse customer : responseA.getBody()) {
            assertThat(customer.companyId()).as(
                            "All customers visible to User A should belong to Company A")
                    .hasValue(companyA.id());
        }
        // Verify User A can see the specific emails
        assertThat(responseA.getBody()).extracting(CustomerResponse::email)
                .containsExactlyInAnyOrder("usera@test.com", "extra1a@test.com",
                        "extra2a@test.com");

        // Verify: User B sees only Company B customers (themselves + 1 extra)
        var responseB = get("/v1/customer/all", tokenB, CustomerResponse[].class);
        assertThat(responseB.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseB.getBody()).as("User B should see exactly 2 customers from Company B")
                .hasSize(2);
        for (CustomerResponse customer : responseB.getBody()) {
            assertThat(customer.companyId()).as(
                            "All customers visible to User B should belong to Company B")
                    .hasValue(companyB.id());
        }
        // Verify User B can see the specific emails
        assertThat(responseB.getBody()).extracting(CustomerResponse::email)
                .containsExactlyInAnyOrder("userb@test.com", "extra1b@test.com");

        // Cross-verification: User A should NOT see any Company B users
        for (CustomerResponse customer : responseA.getBody()) {
            assertThat(customer.email()).as("User A should not see Company B users")
                    .doesNotContain("userb", "extra1b");
        }

        // Cross-verification: User B should NOT see any Company A users
        for (CustomerResponse customer : responseB.getBody()) {
            assertThat(customer.email()).as("User B should not see Company A users")
                    .doesNotContain("usera", "extra1a", "extra2a");
        }

        // Cross-verification: Neither should see the unassigned user
        for (CustomerResponse customer : responseA.getBody()) {
            assertThat(customer.email()).as("User A should not see unassigned user")
                    .isNotEqualTo("nocompany@test.com");
        }
        for (CustomerResponse customer : responseB.getBody()) {
            assertThat(customer.email()).as("User B should not see unassigned user")
                    .isNotEqualTo("nocompany@test.com");
        }
    }

    private void createUserForCompany(UUID companyId, String firstName, String email) {
        CustomerEntity customerEntity = new CustomerEntity();
        customerEntity.setFirstName(firstName);
        customerEntity.setLastName("Test");
        customerEntity.setEmail(email);
        customerEntity.setPassword(UUID.randomUUID().toString());
        customerEntity.setCompanyId(companyId);
        customerRepository.save(customerEntity);
    }
}
