package io.hephaistos.flagforge.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a TemplateField has the required constraints for its type: - STRING: minLength,
 * maxLength - NUMBER: minValue, maxValue, incrementAmount - ENUM: options
 */
@Documented
@Constraint(validatedBy = TemplateFieldValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTemplateField {
    String message() default "Invalid template field constraints";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
