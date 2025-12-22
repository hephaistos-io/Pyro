package io.hephaistos.flagforge.common.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.hephaistos.flagforge.common.enums.FieldType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * A template field definition for NUMBER type values.
 * <p>
 * Requires minValue, maxValue, and incrementAmount constraints.
 *
 * @param key             Unique field identifier
 * @param type            Always NUMBER for this record type
 * @param description     Human-readable description of the field
 * @param editable        Whether users can modify this field
 * @param defaultValue    Default numeric value for this field
 * @param minValue        Minimum allowed value
 * @param maxValue        Maximum allowed value
 * @param incrementAmount Step size for value changes (> 0)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A numeric field with range and increment constraints")
public record NumberTemplateField(@NotBlank(message = "Field key is required") String key,

                                  @Schema(description = "Field type, always NUMBER for this schema") FieldType type,

                                  String description,

                                  boolean editable,

                                  Number defaultValue,

                                  @NotNull(
                                          message = "minValue is required for NUMBER fields") Double minValue,

                                  @NotNull(
                                          message = "maxValue is required for NUMBER fields") Double maxValue,

                                  @NotNull(
                                          message = "incrementAmount is required for NUMBER fields") @Positive(
                                          message = "incrementAmount must be positive") Double incrementAmount)
        implements TemplateField {

    /**
     * Compact constructor that enforces type and validates constraints.
     */
    public NumberTemplateField {
        type = FieldType.NUMBER;
        if (minValue != null && maxValue != null && minValue > maxValue) {
            throw new IllegalArgumentException("minValue must be less than or equal to maxValue");
        }
        // Validate default value against constraints
        if (defaultValue != null) {
            double val = defaultValue.doubleValue();
            if (minValue != null && val < minValue) {
                throw new IllegalArgumentException(
                        "defaultValue (" + val + ") must be at least minValue (" + minValue + ")");
            }
            if (maxValue != null && val > maxValue) {
                throw new IllegalArgumentException(
                        "defaultValue (" + val + ") must be at most maxValue (" + maxValue + ")");
            }
            // Validate increment alignment
            if (incrementAmount != null && minValue != null) {
                double diff = val - minValue;
                double remainder = Math.abs(diff % incrementAmount);
                double tolerance = incrementAmount * 1e-9;
                if (remainder > tolerance && remainder < incrementAmount - tolerance) {
                    throw new IllegalArgumentException(
                            "defaultValue (" + val + ") must align with incrementAmount (" + incrementAmount + ") starting from minValue (" + minValue + ")");
                }
            }
        }
    }

    /**
     * Convenience constructor without explicit type parameter.
     */
    public NumberTemplateField(String key, String description, boolean editable,
            Number defaultValue, Double minValue, Double maxValue, Double incrementAmount) {
        this(key, FieldType.NUMBER, description, editable, defaultValue, minValue, maxValue,
                incrementAmount);
    }
}
