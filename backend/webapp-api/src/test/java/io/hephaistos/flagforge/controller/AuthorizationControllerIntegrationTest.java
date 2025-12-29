package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.IntegrationTestSupport;
import io.hephaistos.flagforge.MailpitTestConfiguration;
import io.hephaistos.flagforge.PostgresTestContainerConfiguration;
import io.hephaistos.flagforge.RedisTestContainerConfiguration;
import io.hephaistos.flagforge.controller.dto.CustomerAuthenticationRequest;
import io.hephaistos.flagforge.controller.dto.CustomerRegistrationRequest;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.data.repository.RegistrationVerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestContainerConfiguration.class, RedisTestContainerConfiguration.class,
        MailpitTestConfiguration.class})
@Tag("integration")
class AuthorizationControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CustomerRepository testCustomerRepository;

    @Autowired
    private RegistrationVerificationTokenRepository verificationTokenRepository;

    @BeforeEach
    void beforeEach() {
        initializeTestSupport();
        verificationTokenRepository.deleteAll();
        testCustomerRepository.deleteAll();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"john.doe@example.com", "john.doe+test@example.com", "john@mail.example.com",
                    "user@localhost.localdomain", "test_user@example.co.uk",
                    "user.name+tag@example.com", "john@example"})
    void validEmailReturns201CreatedAndPersistsToDatabase(String email) {
        var request = CustomerRegistrationRequest.withEmail("John", "Doe", email, "password123");

        var response = post("/v1/auth/register", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Verify the customer was actually persisted to the database
        assertThat(testCustomerRepository.findByEmail(email)).isPresent();
        assertThat(testCustomerRepository.findByEmail(email).get().getFirstName()).isEqualTo(
                "John");
        assertThat(testCustomerRepository.findByEmail(email).get().getLastName()).isEqualTo("Doe");
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-email", "john@", "@example.com", "john doe@example.com",
            "john@@example.com", "john@.com"})
    void invalidEmailFormatReturns400BadRequestAndDoesNotPersist(String email) {
        var request = CustomerRegistrationRequest.withEmail("John", "Doe", email, "password123");

        var response = post("/v1/auth/register", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("email");

        // Verify no customer was persisted to the database
        assertThat(testCustomerRepository.findAll()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @NullSource
    void blankOrNullEmailReturns400BadRequestAndDoesNotPersist(String email) {
        var request = CustomerRegistrationRequest.withEmail("John", "Doe", email, "password123");

        var response = post("/v1/auth/register", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Verify no customer was persisted to the database
        assertThat(testCustomerRepository.findAll()).isEmpty();
    }

    @Test
    void multipleValidEmailsArePersisted() {
        String[] validEmails = {"user1@example.com", "user2@test.org", "user3@mail.co.uk"};

        for (String email : validEmails) {
            var request =
                    CustomerRegistrationRequest.withEmail("John", "Doe", email, "password123");

            var response = post("/v1/auth/register", request, Void.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        // Verify all customers were persisted
        assertThat(testCustomerRepository.findAll()).hasSize(3);
    }

    @Nested
    class EmailVerification {

        @Test
        void newlyRegisteredUserHasUnverifiedEmail() {
            var email = "unverified@example.com";
            var request =
                    CustomerRegistrationRequest.withEmail("John", "Doe", email, "password123");

            post("/v1/auth/register", request, Void.class);

            var customer = testCustomerRepository.findByEmail(email);
            assertThat(customer).isPresent();
            assertThat(customer.get().isEmailVerified()).isFalse();
        }

        @Test
        void registrationCreatesVerificationToken() {
            var email = "tokentest@example.com";
            var request =
                    CustomerRegistrationRequest.withEmail("John", "Doe", email, "password123");

            post("/v1/auth/register", request, Void.class);

            var customer = testCustomerRepository.findByEmail(email);
            assertThat(customer).isPresent();
            var tokenEntity = verificationTokenRepository.findByCustomerId(customer.get().getId());
            assertThat(tokenEntity).isPresent();
        }

        @Test
        void unverifiedUserCannotLogin() {
            var email = "unverified-login@example.com";
            var password = "password123";
            var regRequest = CustomerRegistrationRequest.withEmail("John", "Doe", email, password);
            post("/v1/auth/register", regRequest, Void.class);

            // Don't verify email - try to login directly
            var authRequest = new CustomerAuthenticationRequest(email, password);
            var response = post("/v1/auth/authenticate", authRequest, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).contains("verify");
        }

        @Test
        void verifyEmailWithValidToken() {
            var email = "verify-test@example.com";
            var regRequest =
                    CustomerRegistrationRequest.withEmail("John", "Doe", email, "password123");
            post("/v1/auth/register", regRequest, Void.class);

            // Get the token from database
            var customer = testCustomerRepository.findByEmail(email).orElseThrow();
            var tokenEntity = verificationTokenRepository.findByCustomerId(customer.getId());
            assertThat(tokenEntity).isPresent();
            var token = tokenEntity.get().getToken();

            // Verify email
            var response = post("/v1/auth/verify-registration?token=" + token, null, Void.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            var verifiedCustomer = testCustomerRepository.findByEmail(email).orElseThrow();
            assertThat(verifiedCustomer.isEmailVerified()).isTrue();
        }

        @Test
        void verifyEmailWithInvalidTokenReturns400() {
            var response =
                    post("/v1/auth/verify-registration?token=invalid-token", null, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void verifiedUserCanLogin() {
            var email = "verified-login@example.com";
            var password = "password123";
            var regRequest = CustomerRegistrationRequest.withEmail("John", "Doe", email, password);
            post("/v1/auth/register", regRequest, Void.class);

            // Verify email
            var customer = testCustomerRepository.findByEmail(email).orElseThrow();
            var tokenEntity =
                    verificationTokenRepository.findByCustomerId(customer.getId()).orElseThrow();
            post("/v1/auth/verify-registration?token=" + tokenEntity.getToken(), null, Void.class);

            // Login should succeed
            var authRequest = new CustomerAuthenticationRequest(email, password);
            var response = post("/v1/auth/authenticate", authRequest, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void tokenCanOnlyBeUsedOnce() {
            var email = "single-use@example.com";
            var regRequest =
                    CustomerRegistrationRequest.withEmail("John", "Doe", email, "password123");
            post("/v1/auth/register", regRequest, Void.class);

            var customer = testCustomerRepository.findByEmail(email).orElseThrow();
            var tokenEntity =
                    verificationTokenRepository.findByCustomerId(customer.getId()).orElseThrow();
            var token = tokenEntity.getToken();

            // First use should succeed
            var firstResponse =
                    post("/v1/auth/verify-registration?token=" + token, null, Void.class);
            assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            // Second use should fail
            var secondResponse =
                    post("/v1/auth/verify-registration?token=" + token, null, String.class);
            assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}
