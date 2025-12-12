package io.hephaistos.flagforge.controller.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ApplicationCreationRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"My App", "ab", "Application Name", "test-app-123", "A1"})
    void validNamePassesValidation(String name) {
        ApplicationCreationRequest request = new ApplicationCreationRequest(name);

        Set<ConstraintViolation<ApplicationCreationRequest>> violations =
                validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", ""})
    void tooShortNameFailsValidation(String name) {
        ApplicationCreationRequest request = new ApplicationCreationRequest(name);

        Set<ConstraintViolation<ApplicationCreationRequest>> violations =
                validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations).allMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"   "})
    @NullSource
    void blankOrNullNameFailsValidation(String name) {
        ApplicationCreationRequest request = new ApplicationCreationRequest(name);

        Set<ConstraintViolation<ApplicationCreationRequest>> violations =
                validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations).allMatch(v -> v.getPropertyPath().toString().equals("name"));
    }
}
