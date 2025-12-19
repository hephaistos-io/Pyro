package io.hephaistos.flagforge.validation;

import io.hephaistos.flagforge.data.FieldType;
import io.hephaistos.flagforge.data.TemplateField;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class TemplateFieldValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    // ========== STRING Type Tests ==========

    @Test
    void stringFieldWithValidConstraintsIsValid() {
        var field = new TemplateField("name", FieldType.STRING, "User name", true, "default",
                Map.of("minLength", 0, "maxLength", 100));

        Set<ConstraintViolation<TemplateField>> violations = validator.validate(field);

        assertThat(violations).isEmpty();
    }

    @Test
    void stringFieldWithoutConstraintsIsInvalid() {
        var field = new TemplateField("name", FieldType.STRING, "User name", true, "default", null);

        Set<ConstraintViolation<TemplateField>> violations = validator.validate(field);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("STRING")
                .contains("minLength")
                .contains("maxLength");
    }

    @Test
    void stringFieldWithoutMinLengthIsInvalid() {
        var field = new TemplateField("name", FieldType.STRING, "User name", true, "default",
                Map.of("maxLength", 100));

        Set<ConstraintViolation<TemplateField>> violations = validator.validate(field);

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("minLength");
    }

    @Test
    void stringFieldWithoutMaxLengthIsInvalid() {
        var field = new TemplateField("name", FieldType.STRING, "User name", true, "default",
                Map.of("minLength", 0));

        Set<ConstraintViolation<TemplateField>> violations = validator.validate(field);

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("maxLength");
    }

    @Test
    void stringFieldWithNegativeMinLengthIsInvalid() {
        var field = new TemplateField("name", FieldType.STRING, "User name", true, "default",
                Map.of("minLength", -1, "maxLength", 100));

        Set<ConstraintViolation<TemplateField>> violations = validator.validate(field);

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("minLength")
                .contains("non-negative");
    }

    @Test
    void stringFieldWithMinLengthGreaterThanMaxLengthIsInvalid() {
        var field = new TemplateField("name", FieldType.STRING, "User name", true, "default",
                Map.of("minLength", 100, "maxLength", 50));

        Set<ConstraintViolation<TemplateField>> violations = validator.validate(field);

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("less than or equal");
    }

    // ========== NUMBER Type Tests ==========

    @Test
    void numberFieldWithValidConstraintsIsValid() {
        var field = new TemplateField("amount", FieldType.NUMBER, "Amount", true, 10,
                Map.of("minValue", 0, "maxValue", 100, "incrementAmount", 1));

        Set<ConstraintViolation<TemplateField>> violations = validator.validate(field);

        assertThat(violations).isEmpty();
    }

    @Test
    void numberFieldWithDecimalConstraintsIsValid() {
        var field = new TemplateField("price", FieldType.NUMBER, "Price", true, 9.99,
                Map.of("minValue", 0.0, "maxValue", 999.99, "incrementAmount", 0.01));

        Set<ConstraintViolation<TemplateField>> violations = validator.validate(field);

        assertThat(violations).isEmpty();
    }

    @Test
    void numberFieldWithoutConstraintsIsInvalid() {
        var field = new TemplateField("amount", FieldType.NUMBER, "Amount", true, 10, null);

        Set<ConstraintViolation<TemplateField>> violations = validator.validate(field);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("NUMBER")
                .contains("minValue")
                .contains("maxValue")
                .contains("incrementAmount");
    }

    @Test
    void numberFieldWithoutMinValueIsInvalid() {
        var field = new TemplateField("amount", FieldType.NUMBER, "Amount", true, 10,
                Map.of("maxValue", 100, "incrementAmount", 1));

        Set<ConstraintViolation<TemplateField>> violations = validator.validate(field);

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("minValue");
    }

    @Test
    void numberFieldWithZeroIncrementAmountIsInvalid() {
        var field = new TemplateField("amount", FieldType.NUMBER, "Amount", true, 10,
                Map.of("minValue", 0, "maxValue", 100, "incrementAmount", 0));

        Set<ConstraintViolation<TemplateField>> violations = validator.validate(field);

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("incrementAmount")
                .contains("positive");
    }

    @Test
    void numberFieldWithMinValueGreaterThanMaxValueIsInvalid() {
        var field = new TemplateField("amount", FieldType.NUMBER, "Amount", true, 10,
                Map.of("minValue", 100, "maxValue", 0, "incrementAmount", 1));

        Set<ConstraintViolation<TemplateField>> violations = validator.validate(field);

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("less than or equal");
    }

    // ========== ENUM Type Tests ==========

    @Test
    void enumFieldWithValidOptionsIsValid() {
        var field = new TemplateField("tier", FieldType.ENUM, "Subscription tier", false, "free",
                Map.of("options", List.of("free", "pro", "enterprise")));

        Set<ConstraintViolation<TemplateField>> violations = validator.validate(field);

        assertThat(violations).isEmpty();
    }

    @Test
    void enumFieldWithoutConstraintsIsInvalid() {
        var field =
                new TemplateField("tier", FieldType.ENUM, "Subscription tier", false, "free", null);

        Set<ConstraintViolation<TemplateField>> violations = validator.validate(field);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("ENUM").contains("options");
    }

    @Test
    void enumFieldWithoutOptionsKeyIsInvalid() {
        var field = new TemplateField("tier", FieldType.ENUM, "Subscription tier", false, "free",
                Map.of("other", "value"));

        Set<ConstraintViolation<TemplateField>> violations = validator.validate(field);

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("options");
    }

    @Test
    void enumFieldWithEmptyOptionsIsInvalid() {
        var field = new TemplateField("tier", FieldType.ENUM, "Subscription tier", false, "free",
                Map.of("options", List.of()));

        Set<ConstraintViolation<TemplateField>> violations = validator.validate(field);

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("options")
                .contains("not be empty");
    }

    @Test
    void enumFieldWithNonStringOptionsIsInvalid() {
        var field = new TemplateField("tier", FieldType.ENUM, "Subscription tier", false, "free",
                Map.of("options", List.of(1, 2, 3)));

        Set<ConstraintViolation<TemplateField>> violations = validator.validate(field);

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("options")
                .contains("strings");
    }

    // ========== BOOLEAN Type Tests ==========

    @Test
    void booleanFieldWithoutConstraintsIsValid() {
        var field =
                new TemplateField("night_mode", FieldType.BOOLEAN, "Dark theme", true, false, null);

        Set<ConstraintViolation<TemplateField>> violations = validator.validate(field);

        assertThat(violations).isEmpty();
    }

    @Test
    void booleanFieldWithConstraintsIsStillValid() {
        var field = new TemplateField("night_mode", FieldType.BOOLEAN, "Dark theme", true, false,
                Map.of("extra", "ignored"));

        Set<ConstraintViolation<TemplateField>> violations = validator.validate(field);

        assertThat(violations).isEmpty();
    }
}
