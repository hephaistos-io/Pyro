package io.hephaistos.flagforge.validation;

import io.hephaistos.flagforge.common.enums.FieldType;
import io.hephaistos.flagforge.common.types.BooleanTemplateField;
import io.hephaistos.flagforge.common.types.EnumTemplateField;
import io.hephaistos.flagforge.common.types.NumberTemplateField;
import io.hephaistos.flagforge.common.types.StringTemplateField;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for polymorphic TemplateField validation using Jakarta Bean Validation. Each type-specific
 * record (StringTemplateField, NumberTemplateField, etc.) is validated using its own bean
 * validation annotations.
 */
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
        var field = new StringTemplateField("name", "User name", true, "default", 0, 100);

        Set<ConstraintViolation<StringTemplateField>> violations = validator.validate(field);

        assertThat(violations).isEmpty();
        assertThat(field.type()).isEqualTo(FieldType.STRING);
    }

    @Test
    void stringFieldWithNullMinLengthIsInvalid() {
        var field = new StringTemplateField("name", FieldType.STRING, "User name", true, "default",
                null, 100);

        Set<ConstraintViolation<StringTemplateField>> violations = validator.validate(field);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("minLength");
    }

    @Test
    void stringFieldWithNullMaxLengthIsInvalid() {
        var field =
                new StringTemplateField("name", FieldType.STRING, "User name", true, "default", 0,
                        null);

        Set<ConstraintViolation<StringTemplateField>> violations = validator.validate(field);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("maxLength");
    }

    @Test
    void stringFieldWithNegativeMinLengthIsInvalid() {
        var field =
                new StringTemplateField("name", FieldType.STRING, "User name", true, "default", -1,
                        100);

        Set<ConstraintViolation<StringTemplateField>> violations = validator.validate(field);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("non-negative");
    }

    @Test
    void stringFieldWithZeroMaxLengthIsInvalid() {
        var field = new StringTemplateField("name", FieldType.STRING, "User name", true, null, 0,
                        0);

        Set<ConstraintViolation<StringTemplateField>> violations = validator.validate(field);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("positive");
    }

    @Test
    void stringFieldWithMinLengthGreaterThanMaxLengthThrowsException() {
        assertThatThrownBy(() -> new StringTemplateField("name", "User name", true, "default", 100,
                50)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minLength")
                .hasMessageContaining("less than or equal");
    }

    @Test
    void stringFieldWithDefaultValueTooShortThrowsException() {
        assertThatThrownBy(() -> new StringTemplateField("name", "User name", true, "ab", 5,
                100)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("defaultValue")
                .hasMessageContaining("at least")
                .hasMessageContaining("5");
    }

    @Test
    void stringFieldWithDefaultValueTooLongThrowsException() {
        assertThatThrownBy(
                () -> new StringTemplateField("name", "User name", true, "this is way too long", 0,
                        10)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("defaultValue")
                .hasMessageContaining("at most")
                .hasMessageContaining("10");
    }

    @Test
    void stringFieldWithDefaultValueWithinConstraintsIsValid() {
        var field = new StringTemplateField("name", "User name", true, "hello", 3, 10);

        Set<ConstraintViolation<StringTemplateField>> violations = validator.validate(field);

        assertThat(violations).isEmpty();
    }

    @Test
    void stringFieldWithEmptyDefaultValueIsValidRegardlessOfMinLength() {
        // Empty default value should be allowed (it means "no default")
        var field = new StringTemplateField("name", "User name", true, "", 5, 100);

        Set<ConstraintViolation<StringTemplateField>> violations = validator.validate(field);

        assertThat(violations).isEmpty();
    }

    @Test
    void stringFieldWithBlankKeyIsInvalid() {
        var field =
                new StringTemplateField("", FieldType.STRING, "User name", true, "default", 0, 100);

        Set<ConstraintViolation<StringTemplateField>> violations = validator.validate(field);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("key");
    }

    // ========== NUMBER Type Tests ==========

    @Test
    void numberFieldWithValidConstraintsIsValid() {
        var field = new NumberTemplateField("amount", "Amount", true, 10, 0.0, 100.0, 1.0);

        Set<ConstraintViolation<NumberTemplateField>> violations = validator.validate(field);

        assertThat(violations).isEmpty();
        assertThat(field.type()).isEqualTo(FieldType.NUMBER);
    }

    @Test
    void numberFieldWithDecimalConstraintsIsValid() {
        var field = new NumberTemplateField("price", "Price", true, 9.99, 0.0, 999.99, 0.01);

        Set<ConstraintViolation<NumberTemplateField>> violations = validator.validate(field);

        assertThat(violations).isEmpty();
    }

    @Test
    void numberFieldWithNullMinValueIsInvalid() {
        var field =
                new NumberTemplateField("amount", FieldType.NUMBER, "Amount", true, 10, null, 100.0,
                        1.0);

        Set<ConstraintViolation<NumberTemplateField>> violations = validator.validate(field);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("minValue");
    }

    @Test
    void numberFieldWithNullMaxValueIsInvalid() {
        var field =
                new NumberTemplateField("amount", FieldType.NUMBER, "Amount", true, 10, 0.0, null,
                        1.0);

        Set<ConstraintViolation<NumberTemplateField>> violations = validator.validate(field);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("maxValue");
    }

    @Test
    void numberFieldWithNullIncrementAmountIsInvalid() {
        var field =
                new NumberTemplateField("amount", FieldType.NUMBER, "Amount", true, 10, 0.0, 100.0,
                        null);

        Set<ConstraintViolation<NumberTemplateField>> violations = validator.validate(field);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("incrementAmount");
    }

    @Test
    void numberFieldWithZeroIncrementAmountIsInvalid() {
        var field =
                new NumberTemplateField("amount", FieldType.NUMBER, "Amount", true, 10, 0.0, 100.0,
                        0.0);

        Set<ConstraintViolation<NumberTemplateField>> violations = validator.validate(field);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("incrementAmount")
                .contains("positive");
    }

    @Test
    void numberFieldWithNegativeIncrementAmountIsInvalid() {
        var field =
                new NumberTemplateField("amount", FieldType.NUMBER, "Amount", true, 10, 0.0, 100.0,
                        -1.0);

        Set<ConstraintViolation<NumberTemplateField>> violations = validator.validate(field);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("positive");
    }

    @Test
    void numberFieldWithMinValueGreaterThanMaxValueThrowsException() {
        assertThatThrownBy(() -> new NumberTemplateField("amount", "Amount", true, 10, 100.0, 0.0,
                1.0)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minValue")
                .hasMessageContaining("less than or equal");
    }

    @Test
    void numberFieldWithDefaultValueBelowMinValueThrowsException() {
        assertThatThrownBy(() -> new NumberTemplateField("amount", "Amount", true, -5, 0.0, 100.0,
                1.0)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("defaultValue")
                .hasMessageContaining("at least")
                .hasMessageContaining("0.0");
    }

    @Test
    void numberFieldWithDefaultValueAboveMaxValueThrowsException() {
        assertThatThrownBy(() -> new NumberTemplateField("amount", "Amount", true, 150, 0.0, 100.0,
                1.0)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("defaultValue")
                .hasMessageContaining("at most")
                .hasMessageContaining("100.0");
    }

    @Test
    void numberFieldWithDefaultValueNotAlignedWithIncrementThrowsException() {
        // minValue=0, increment=0.2, so valid values are 0, 0.2, 0.4, etc.
        // 0.1 is not valid
        assertThatThrownBy(() -> new NumberTemplateField("amount", "Amount", true, 0.1, 0.0, 10.0,
                0.2)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("defaultValue")
                .hasMessageContaining("align")
                .hasMessageContaining("incrementAmount");
    }

    @Test
    void numberFieldWithDefaultValueAlignedWithIncrementIsValid() {
        // minValue=0, increment=0.2, defaultValue=0.4 is valid
        var field = new NumberTemplateField("amount", "Amount", true, 0.4, 0.0, 10.0, 0.2);

        Set<ConstraintViolation<NumberTemplateField>> violations = validator.validate(field);

        assertThat(violations).isEmpty();
    }

    @Test
    void numberFieldWithDefaultValueAtMinValueIsValid() {
        var field = new NumberTemplateField("amount", "Amount", true, 0.0, 0.0, 100.0, 1.0);

        Set<ConstraintViolation<NumberTemplateField>> violations = validator.validate(field);

        assertThat(violations).isEmpty();
    }

    @Test
    void numberFieldWithDefaultValueAtMaxValueIsValid() {
        var field = new NumberTemplateField("amount", "Amount", true, 100.0, 0.0, 100.0, 1.0);

        Set<ConstraintViolation<NumberTemplateField>> violations = validator.validate(field);

        assertThat(violations).isEmpty();
    }

    @Test
    void numberFieldWithNullDefaultValueIsValid() {
        var field = new NumberTemplateField("amount", "Amount", true, null, 0.0, 100.0, 1.0);

        Set<ConstraintViolation<NumberTemplateField>> violations = validator.validate(field);

        assertThat(violations).isEmpty();
    }

    // ========== ENUM Type Tests ==========

    @Test
    void enumFieldWithValidOptionsIsValid() {
        var field = new EnumTemplateField("tier", "Subscription tier", false, "free",
                List.of("free", "pro", "enterprise"));

        Set<ConstraintViolation<EnumTemplateField>> violations = validator.validate(field);

        assertThat(violations).isEmpty();
        assertThat(field.type()).isEqualTo(FieldType.ENUM);
    }

    @Test
    void enumFieldWithNullOptionsIsInvalid() {
        var field =
                new EnumTemplateField("tier", FieldType.ENUM, "Subscription tier", false, "free",
                        null);

        Set<ConstraintViolation<EnumTemplateField>> violations = validator.validate(field);

        // Both @NotNull and @NotEmpty trigger for null, so we get 2 violations
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("options"));
    }

    @Test
    void enumFieldWithEmptyOptionsIsInvalid() {
        var field =
                new EnumTemplateField("tier", FieldType.ENUM, "Subscription tier", false, "free",
                        List.of());

        Set<ConstraintViolation<EnumTemplateField>> violations = validator.validate(field);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("not be empty");
    }

    @Test
    void enumFieldOptionsAreImmutable() {
        var options = new java.util.ArrayList<>(List.of("a", "b"));
        var field = new EnumTemplateField("tier", "Tier", false, "a", options);

        // Try to modify after creation
        assertThatThrownBy(() -> field.options().add("c")).isInstanceOf(
                UnsupportedOperationException.class);
    }

    @Test
    void enumFieldWithNullDefaultValueIsValid() {
        var field = new EnumTemplateField("tier", "Subscription tier", false, null,
                List.of("free", "pro", "enterprise"));

        Set<ConstraintViolation<EnumTemplateField>> violations = validator.validate(field);

        assertThat(violations).isEmpty();
    }

    @Test
    void enumFieldWithBlankKeyIsInvalid() {
        var field = new EnumTemplateField("", FieldType.ENUM, "Subscription tier", false, "free",
                List.of("free", "pro"));

        Set<ConstraintViolation<EnumTemplateField>> violations = validator.validate(field);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("key");
    }

    @Test
    void enumFieldWithBlankOptionIsInvalid() {
        var field = new EnumTemplateField("tier", FieldType.ENUM, "Subscription tier", false, "free",
                List.of("free", "", "pro"));

        Set<ConstraintViolation<EnumTemplateField>> violations = validator.validate(field);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("non-blank");
    }

    @Test
    void enumFieldWithSingleOptionIsValid() {
        var field = new EnumTemplateField("status", "Status", false, "active",
                List.of("active"));

        Set<ConstraintViolation<EnumTemplateField>> violations = validator.validate(field);

        assertThat(violations).isEmpty();
    }

    // ========== BOOLEAN Type Tests ==========

    @Test
    void booleanFieldIsValid() {
        var field = new BooleanTemplateField("night_mode", "Dark theme", true, false);

        Set<ConstraintViolation<BooleanTemplateField>> violations = validator.validate(field);

        assertThat(violations).isEmpty();
        assertThat(field.type()).isEqualTo(FieldType.BOOLEAN);
    }

    @Test
    void booleanFieldWithNullDefaultValueIsValid() {
        var field = new BooleanTemplateField("night_mode", "Dark theme", true, null);

        Set<ConstraintViolation<BooleanTemplateField>> violations = validator.validate(field);

        assertThat(violations).isEmpty();
    }

    @Test
    void booleanFieldWithBlankKeyIsInvalid() {
        var field = new BooleanTemplateField("", FieldType.BOOLEAN, "Dark theme", true, false);

        Set<ConstraintViolation<BooleanTemplateField>> violations = validator.validate(field);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("key");
    }
}
