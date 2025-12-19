package io.hephaistos.flagforge.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.hephaistos.flagforge.validation.ValidTemplateField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Represents a single field definition in a template schema.
 * <p>
 * Constraints are required based on field type:
 * <ul>
 *   <li>STRING: minLength (int >= 0), maxLength (int > 0)</li>
 *   <li>NUMBER: minValue (number), maxValue (number), incrementAmount (number > 0)</li>
 *   <li>ENUM: options (non-empty list of strings)</li>
 *   <li>BOOLEAN: no constraints required</li>
 * </ul>
 *
 * @param key          Unique field identifier (e.g., "night_mode", "tier")
 * @param type         Field data type
 * @param description  Human-readable description of the field
 * @param editable     Whether users can modify this field (USER templates only)
 * @param defaultValue Default value for this field
 * @param constraints  Type-specific constraints (required for STRING, NUMBER, ENUM)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@ValidTemplateField
public record TemplateField(@NotBlank(message = "Field key is required") String key,

                            @NotNull(message = "Field type is required") FieldType type,

                            String description,

                            boolean editable,

                            Object defaultValue,

                            Map<String, Object> constraints) {
}
