package io.hephaistos.pyro.controller;

import io.hephaistos.pyro.IntegrationTestSupport;
import io.hephaistos.pyro.controller.dto.UserRegistrationRequest;
import io.hephaistos.pyro.data.repository.UserRepository;
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
    private UserRepository userRepository;

    @BeforeEach
    void beforeEach() {
        initializeTestSupport();
        userRepository.deleteAll();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"john.doe@example.com", "john.doe+test@example.com", "john@mail.example.com",
                    "user@localhost.localdomain", "test_user@example.co.uk",
                    "user.name+tag@example.com", "john@example"})
    void validEmailReturns201CreatedAndPersistsToDatabase(String email) {
        var request = new UserRegistrationRequest("John", "Doe", email, "password123");

        var response = post("/v1/auth/register", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Verify the user was actually persisted to the database
        assertThat(userRepository.findByEmail(email)).isPresent();
        assertThat(userRepository.findByEmail(email).get().getFirstName()).isEqualTo("John");
        assertThat(userRepository.findByEmail(email).get().getLastName()).isEqualTo("Doe");
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-email", "john@", "@example.com", "john doe@example.com",
            "john@@example.com", "john@.com"})
    void invalidEmailFormatReturns400BadRequestAndDoesNotPersist(String email) {
        var request = new UserRegistrationRequest("John", "Doe", email, "password123");

        var response = post("/v1/auth/register", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("email");

        // Verify no user was persisted to the database
        assertThat(userRepository.findAll()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @NullSource
    void blankOrNullEmailReturns400BadRequestAndDoesNotPersist(String email) {
        var request = new UserRegistrationRequest("John", "Doe", email, "password123");

        var response = post("/v1/auth/register", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("email");

        // Verify no user was persisted to the database
        assertThat(userRepository.findAll()).isEmpty();
    }

    @Test
    void multipleValidEmailsArePersisted() {
        String[] validEmails = {"user1@example.com", "user2@test.org", "user3@mail.co.uk"};

        for (String email : validEmails) {
            var request = new UserRegistrationRequest("John", "Doe", email, "password123");

            var response = post("/v1/auth/register", request, Void.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        // Verify all users were persisted
        assertThat(userRepository.findAll()).hasSize(3);
    }
}
