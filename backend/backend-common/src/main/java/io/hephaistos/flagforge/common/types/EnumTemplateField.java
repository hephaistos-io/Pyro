package io.hephaistos.flagforge.common.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.hephaistos.flagforge.common.enums.FieldType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * A template field definition for ENUM type values.
 * <p>
 * Requires a list of allowed options.
 *
 * @param key          Unique field identifier
 * @param type         Always ENUM for this record type
 * @param description  Human-readable description of the field
 * @param editable     Whether users can modify this field
 * @param defaultValue Default option value for this field (must be in options list)
 * @param options      List of allowed string values
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "An enum field with a list of allowed options")
public record EnumTemplateField(@NotBlank(message = "Field key is required") String key,

                                @Schema(description = "Field type, always ENUM for this schema") FieldType type,

                                String description,

                                boolean editable,

                                String defaultValue,

                                @NotNull(message = "options is required for ENUM fields") @NotEmpty(
                                        message = "options must not be empty") List<@NotBlank(
                                        message = "options must contain non-blank strings") String> options)
        implements TemplateField {

    /**
     * Compact constructor that enforces type and creates defensive copy of options.
     */
    public EnumTemplateField {
        type = FieldType.ENUM;
        if (options != null) {
            options = List.copyOf(options);
        }
    }

    /**
     * Convenience constructor without explicit type parameter.
     */
    public EnumTemplateField(String key, String description, boolean editable, String defaultValue,
            List<String> options) {
        this(key, FieldType.ENUM, description, editable, defaultValue, options);
    }
}
