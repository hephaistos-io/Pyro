package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.IntegrationTestSupport;
import io.hephaistos.flagforge.controller.dto.CustomerRegistrationRequest;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("integration")
class AuthorizationControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void beforeEach() {
        initializeTestSupport();
        customerRepository.deleteAll();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"john.doe@example.com", "john.doe+test@example.com", "john@mail.example.com",
                    "user@localhost.localdomain", "test_user@example.co.uk",
                    "user.name+tag@example.com", "john@example"})
    void validEmailReturns201CreatedAndPersistsToDatabase(String email) {
        var request = new CustomerRegistrationRequest("John", "Doe", email, "password123");

        var response = post("/v1/auth/register", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Verify the customer was actually persisted to the database
        assertThat(customerRepository.findByEmail(email)).isPresent();
        assertThat(customerRepository.findByEmail(email).get().getFirstName()).isEqualTo("John");
        assertThat(customerRepository.findByEmail(email).get().getLastName()).isEqualTo("Doe");
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-email", "john@", "@example.com", "john doe@example.com",
            "john@@example.com", "john@.com"})
    void invalidEmailFormatReturns400BadRequestAndDoesNotPersist(String email) {
        var request = new CustomerRegistrationRequest("John", "Doe", email, "password123");

        var response = post("/v1/auth/register", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("email");

        // Verify no customer was persisted to the database
        assertThat(customerRepository.findAll()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @NullSource
    void blankOrNullEmailReturns400BadRequestAndDoesNotPersist(String email) {
        var request = new CustomerRegistrationRequest("John", "Doe", email, "password123");

        var response = post("/v1/auth/register", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("email");

        // Verify no customer was persisted to the database
        assertThat(customerRepository.findAll()).isEmpty();
    }

    @Test
    void multipleValidEmailsArePersisted() {
        String[] validEmails = {"user1@example.com", "user2@test.org", "user3@mail.co.uk"};

        for (String email : validEmails) {
            var request = new CustomerRegistrationRequest("John", "Doe", email, "password123");

            var response = post("/v1/auth/register", request, Void.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        // Verify all customers were persisted
        assertThat(customerRepository.findAll()).hasSize(3);
    }
}
