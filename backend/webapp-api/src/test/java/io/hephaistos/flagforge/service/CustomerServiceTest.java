package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.MockPasswordCheck;
import io.hephaistos.flagforge.controller.dto.CustomerRegistrationRequest;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CustomerServiceTest extends MockPasswordCheck {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void beforeEach() {
        customerRepository.deleteAll();
        mockPasswordBreachCheckWithResponse(false);
    }

    @Test
    void newCustomersArePersisted() {
        customerService.registerCustomer(newCustomerRegistrationRequest());
        assertThat(customerRepository.findAll()).hasSize(1);
    }

    @Test
    void multipleCustomersArePersistedIfEmailIsDifferent() {
        customerService.registerCustomer(newCustomerRegistrationRequest());
        customerService.registerCustomer(newCustomerRegistrationRequest("different@mail.com"));
        customerService.registerCustomer(newCustomerRegistrationRequest("another@mail.com"));
        assertThat(customerRepository.findAll()).hasSize(3);
    }

    @Test
    void newUserWithExistingEmailThrowsException() {
        customerService.registerCustomer(newCustomerRegistrationRequest());
        assertThatThrownBy(() -> customerService.registerCustomer(newCustomerRegistrationRequest()),
                "Email already exists");
    }


    private CustomerRegistrationRequest newCustomerRegistrationRequest() {
        return newCustomerRegistrationRequest("name@domain.com");
    }

    private CustomerRegistrationRequest newCustomerRegistrationRequest(String email) {
        return new CustomerRegistrationRequest("FirstName", "LastName", email, "123456");
    }

}
