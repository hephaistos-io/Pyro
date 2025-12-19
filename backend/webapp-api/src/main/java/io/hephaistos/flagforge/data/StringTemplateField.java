package io.hephaistos.flagforge.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * A template field definition for STRING type values.
 * <p>
 * Requires minLength and maxLength constraints to define valid string lengths.
 *
 * @param key          Unique field identifier
 * @param type         Always STRING for this record type
 * @param description  Human-readable description of the field
 * @param editable     Whether users can modify this field
 * @param defaultValue Default string value for this field
 * @param minLength    Minimum allowed string length (>= 0)
 * @param maxLength    Maximum allowed string length (> 0)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A string field with length constraints")
public record StringTemplateField(@NotBlank(message = "Field key is required") String key,

                                  @Schema(description = "Field type, always STRING for this schema") FieldType type,

                                  String description,

                                  boolean editable,

                                  String defaultValue,

                                  @NotNull(
                                          message = "minLength is required for STRING fields") @Min(
                                          value = 0,
                                          message = "minLength must be non-negative") Integer minLength,

                                  @NotNull(
                                          message = "maxLength is required for STRING fields") @Positive(
                                          message = "maxLength must be positive") Integer maxLength)
        implements TemplateField {

    /**
     * Compact constructor that enforces type and validates constraints.
     */
    public StringTemplateField {
        type = FieldType.STRING;
        if (minLength != null && maxLength != null && minLength > maxLength) {
            throw new IllegalArgumentException("minLength must be less than or equal to maxLength");
        }
        // Validate default value against constraints
        if (defaultValue != null && !defaultValue.isEmpty()) {
            if (minLength != null && defaultValue.length() < minLength) {
                throw new IllegalArgumentException(
                        "defaultValue length (" + defaultValue.length() + ") must be at least minLength (" + minLength + ")");
            }
            if (maxLength != null && defaultValue.length() > maxLength) {
                throw new IllegalArgumentException(
                        "defaultValue length (" + defaultValue.length() + ") must be at most maxLength (" + maxLength + ")");
            }
        }
    }

    /**
     * Convenience constructor without explicit type parameter.
     */
    public StringTemplateField(String key, String description, boolean editable,
            String defaultValue, Integer minLength, Integer maxLength) {
        this(key, FieldType.STRING, description, editable, defaultValue, minLength, maxLength);
    }
}
