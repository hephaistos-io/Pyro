package io.hephaistos.flagforge.data;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents the schema structure of a template, containing field definitions. This is serialized
 * as JSONB in the database.
 *
 * @param fields List of field definitions (can be empty for newly created templates)
 */
public record TemplateSchema(@Valid List<TemplateField> fields) {
    /**
     * Extracts default values from all fields as a map of key to defaultValue.
     *
     * @return Map of field key to default value (excludes fields with null defaults)
     */
    public Map<String, Object> getDefaultValues() {
        return fields.stream()
                .filter(f -> f.defaultValue() != null)
                .collect(Collectors.toMap(TemplateField::key, TemplateField::defaultValue));
    }
}
