package io.hephaistos.flagforge.controller.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class CustomerRegistrationRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"john.doe@example.com", "john.doe+test@example.com", "john@mail.example.com",
                    "user@localhost.localdomain", "test_user@example.co.uk",
                    "user.name+tag@example.com", "john@example"})
    void validEmailPassesValidation(String email) {
        CustomerRegistrationRequest request =
                CustomerRegistrationRequest.withEmail("John", "Doe", email, "password123");

        Set<ConstraintViolation<CustomerRegistrationRequest>> violations =
                validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-email", "john@", "@example.com", "john doe@example.com",
            "john@@example.com", "john@.com"})
    void invalidEmailFormatFailsValidation(String email) {
        CustomerRegistrationRequest request =
                CustomerRegistrationRequest.withEmail("John", "Doe", email, "password123");

        Set<ConstraintViolation<CustomerRegistrationRequest>> violations =
                validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations).allMatch(v -> v.getPropertyPath().toString().equals("email"));
    }
}
