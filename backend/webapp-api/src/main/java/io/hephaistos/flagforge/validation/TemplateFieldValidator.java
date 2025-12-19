package io.hephaistos.flagforge.validation;

import io.hephaistos.flagforge.data.TemplateField;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;
import java.util.Map;

/**
 * Validates TemplateField constraints based on field type: - STRING: requires minLength (integer >=
 * 0) and maxLength (integer > minLength) - NUMBER: requires minValue (number), maxValue (number >
 * minValue), incrementAmount (number > 0) - ENUM: requires options (non-empty list of strings) -
 * BOOLEAN: no constraints required
 */
public class TemplateFieldValidator
        implements ConstraintValidator<ValidTemplateField, TemplateField> {

    @Override
    public boolean isValid(TemplateField field, ConstraintValidatorContext context) {
        if (field == null || field.type() == null) {
            return true; // Let @NotNull handle null checks
        }

        context.disableDefaultConstraintViolation();
        Map<String, Object> constraints = field.constraints();

        return switch (field.type()) {
            case STRING -> validateStringConstraints(field.key(), constraints, context);
            case NUMBER -> validateNumberConstraints(field.key(), constraints, context);
            case ENUM -> validateEnumConstraints(field.key(), constraints, context);
            case BOOLEAN -> true; // No constraints required
        };
    }

    private boolean validateStringConstraints(String key, Map<String, Object> constraints,
            ConstraintValidatorContext context) {
        if (constraints == null) {
            addViolation(context,
                    "Field '%s' of type STRING requires constraints with minLength and maxLength".formatted(
                            key));
            return false;
        }

        boolean valid = true;

        if (!constraints.containsKey("minLength")) {
            addViolation(context,
                    "Field '%s' of type STRING requires minLength constraint".formatted(key));
            valid = false;
        }
        else if (!isNonNegativeInteger(constraints.get("minLength"))) {
            addViolation(context,
                    "Field '%s': minLength must be a non-negative integer".formatted(key));
            valid = false;
        }

        if (!constraints.containsKey("maxLength")) {
            addViolation(context,
                    "Field '%s' of type STRING requires maxLength constraint".formatted(key));
            valid = false;
        }
        else if (!isPositiveInteger(constraints.get("maxLength"))) {
            addViolation(context,
                    "Field '%s': maxLength must be a positive integer".formatted(key));
            valid = false;
        }

        // Validate minLength < maxLength if both are valid
        if (valid && constraints.containsKey("minLength") && constraints.containsKey("maxLength")) {
            int minLength = toInt(constraints.get("minLength"));
            int maxLength = toInt(constraints.get("maxLength"));
            if (minLength > maxLength) {
                addViolation(context,
                        "Field '%s': minLength must be less than or equal to maxLength".formatted(
                                key));
                valid = false;
            }
        }

        return valid;
    }

    private boolean validateNumberConstraints(String key, Map<String, Object> constraints,
            ConstraintValidatorContext context) {
        if (constraints == null) {
            addViolation(context,
                    "Field '%s' of type NUMBER requires constraints with minValue, maxValue, and incrementAmount".formatted(
                            key));
            return false;
        }

        boolean valid = true;

        if (!constraints.containsKey("minValue")) {
            addViolation(context,
                    "Field '%s' of type NUMBER requires minValue constraint".formatted(key));
            valid = false;
        }
        else if (isNotNumber(constraints.get("minValue"))) {
            addViolation(context, "Field '%s': minValue must be a number".formatted(key));
            valid = false;
        }

        if (!constraints.containsKey("maxValue")) {
            addViolation(context,
                    "Field '%s' of type NUMBER requires maxValue constraint".formatted(key));
            valid = false;
        }
        else if (isNotNumber(constraints.get("maxValue"))) {
            addViolation(context, "Field '%s': maxValue must be a number".formatted(key));
            valid = false;
        }

        if (!constraints.containsKey("incrementAmount")) {
            addViolation(context,
                    "Field '%s' of type NUMBER requires incrementAmount constraint".formatted(key));
            valid = false;
        }
        else if (!isPositiveNumber(constraints.get("incrementAmount"))) {
            addViolation(context,
                    "Field '%s': incrementAmount must be a positive number".formatted(key));
            valid = false;
        }

        // Validate minValue < maxValue if both are valid
        if (valid && constraints.containsKey("minValue") && constraints.containsKey("maxValue")) {
            double minValue = toDouble(constraints.get("minValue"));
            double maxValue = toDouble(constraints.get("maxValue"));
            if (minValue > maxValue) {
                addViolation(context,
                        "Field '%s': minValue must be less than or equal to maxValue".formatted(
                                key));
                valid = false;
            }
        }

        return valid;
    }

    private boolean validateEnumConstraints(String key, Map<String, Object> constraints,
            ConstraintValidatorContext context) {
        if (constraints == null || !constraints.containsKey("options")) {
            addViolation(context,
                    "Field '%s' of type ENUM requires constraints with options".formatted(key));
            return false;
        }

        Object options = constraints.get("options");
        if (!(options instanceof List<?> optionsList)) {
            addViolation(context, "Field '%s': options must be a list".formatted(key));
            return false;
        }

        if (optionsList.isEmpty()) {
            addViolation(context, "Field '%s': options must not be empty".formatted(key));
            return false;
        }

        for (Object option : optionsList) {
            if (!(option instanceof String)) {
                addViolation(context, "Field '%s': all options must be strings".formatted(key));
                return false;
            }
        }

        return true;
    }

    private void addViolation(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

    private boolean isNonNegativeInteger(Object value) {
        if (value instanceof Integer i)
            return i >= 0;
        if (value instanceof Long l)
            return l >= 0;
        if (value instanceof Number n) {
            double d = n.doubleValue();
            return d >= 0 && d == Math.floor(d);
        }
        return false;
    }

    private boolean isPositiveInteger(Object value) {
        if (value instanceof Integer i)
            return i > 0;
        if (value instanceof Long l)
            return l > 0;
        if (value instanceof Number n) {
            double d = n.doubleValue();
            return d > 0 && d == Math.floor(d);
        }
        return false;
    }

    private boolean isNotNumber(Object value) {
        return !(value instanceof Number);
    }

    private boolean isPositiveNumber(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue() > 0;
        }
        return false;
    }

    private int toInt(Object value) {
        return ((Number) value).intValue();
    }

    private double toDouble(Object value) {
        return ((Number) value).doubleValue();
    }
}
