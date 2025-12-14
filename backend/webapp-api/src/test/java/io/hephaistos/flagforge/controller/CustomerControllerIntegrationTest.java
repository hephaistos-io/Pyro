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
        String token = registerAndAuthenticateWithCompany();
        var company = createCompany(token, "TestCompanyPleaseIgnore");

        createUserForCompany(company.id());
        createUserForCompany(company.id());
        createUserForCompany(company.id());
        var allCustomersResponse = get("/v1/customer/all", token, CustomerResponse[].class);
        assertThat(allCustomersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(allCustomersResponse.getBody()).hasSize(4);
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
