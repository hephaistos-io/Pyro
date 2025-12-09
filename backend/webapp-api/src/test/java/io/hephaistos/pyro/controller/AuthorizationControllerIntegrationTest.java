package io.hephaistos.pyro.controller;

import io.hephaistos.pyro.MockPasswordCheck;
import io.hephaistos.pyro.controller.dto.UserRegistrationRequest;
import io.hephaistos.pyro.data.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("integration")
class AuthorizationControllerIntegrationTest extends MockPasswordCheck {

    @LocalServerPort
    private int port;

    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void beforeEach() {
        restTemplate = new TestRestTemplate();
        userRepository.deleteAll();
        mockPasswordBreachCheckWithResponse(false);
    }

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api";
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"john.doe@example.com", "john.doe+test@example.com", "john@mail.example.com",
                    "user@localhost.localdomain", "test_user@example.co.uk",
                    "user.name+tag@example.com", "john@example"})
    void validEmailReturns201CreatedAndPersistsToDatabase(String email) {
        UserRegistrationRequest request =
                new UserRegistrationRequest("John", "Doe", email, "password123");

        ResponseEntity<Void> response = getResponseEntity(request);

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
        UserRegistrationRequest request =
                new UserRegistrationRequest("John", "Doe", email, "password123");

        ResponseEntity<String> response =
                restTemplate.postForEntity(getBaseUrl() + "/v1/auth/register", request,
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("email");

        // Verify no user was persisted to the database
        assertThat(userRepository.findAll()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @NullSource
    void blankOrNullEmailReturns400BadRequestAndDoesNotPersist(String email) {
        UserRegistrationRequest request =
                new UserRegistrationRequest("John", "Doe", email, "password123");

        ResponseEntity<String> response =
                restTemplate.postForEntity(getBaseUrl() + "/v1/auth/register", request,
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("email");

        // Verify no user was persisted to the database
        assertThat(userRepository.findAll()).isEmpty();
    }

    @Test
    void multipleValidEmailsArePersisted() {
        String[] validEmails = {"user1@example.com", "user2@test.org", "user3@mail.co.uk"};

        for (String email : validEmails) {
            UserRegistrationRequest request =
                    new UserRegistrationRequest("John", "Doe", email, "password123");

            ResponseEntity<Void> response = getResponseEntity(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        // Verify all users were persisted
        assertThat(userRepository.findAll()).hasSize(3);
    }

    private ResponseEntity<Void> getResponseEntity(UserRegistrationRequest request) {
        return restTemplate.postForEntity(getBaseUrl() + "/v1/auth/register", request, Void.class);
    }
}
