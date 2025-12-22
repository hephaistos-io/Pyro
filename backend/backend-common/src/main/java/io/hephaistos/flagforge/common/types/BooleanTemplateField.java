package io.hephaistos.flagforge.common.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.hephaistos.flagforge.common.enums.FieldType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * A template field definition for BOOLEAN type values.
 * <p>
 * Boolean fields have no additional constraints.
 *
 * @param key          Unique field identifier
 * @param type         Always BOOLEAN for this record type
 * @param description  Human-readable description of the field
 * @param editable     Whether users can modify this field
 * @param defaultValue Default boolean value for this field
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A boolean field with no additional constraints")
public record BooleanTemplateField(@NotBlank(message = "Field key is required") String key,

                                   @Schema(description = "Field type, always BOOLEAN for this schema") FieldType type,

                                   String description,

                                   boolean editable,

                                   Boolean defaultValue) implements TemplateField {

    /**
     * Compact constructor that enforces type.
     */
    public BooleanTemplateField {
        type = FieldType.BOOLEAN;
    }

    /**
     * Convenience constructor without explicit type parameter.
     */
    public BooleanTemplateField(String key, String description, boolean editable,
            Boolean defaultValue) {
        this(key, FieldType.BOOLEAN, description, editable, defaultValue);
    }
}
