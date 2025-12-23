package io.hephaistos.flagforge.common.util;

import io.hephaistos.flagforge.common.types.TemplateSchema;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class for merging template schema defaults with override values. Used by both webapp-api
 * and customer-api to ensure consistent merge behavior.
 */
public final class TemplateMerger {

    private TemplateMerger() {
        // Utility class
    }

    /**
     * Merge template schema defaults with a single override's values.
     *
     * @param schema         The template schema containing field definitions with defaults
     * @param overrideValues The override values to apply, or null if no override
     * @return A new map containing merged values (defaults + override)
     */
    public static Map<String, Object> merge(TemplateSchema schema,
            @Nullable Map<String, Object> overrideValues) {
        // Start with defaults from schema (use LinkedHashMap to preserve field order)
        Map<String, Object> mergedValues = new LinkedHashMap<>(schema.getDefaultValues());

        // Apply override values if provided
        if (overrideValues != null && !overrideValues.isEmpty()) {
            mergedValues.putAll(overrideValues);
        }

        return mergedValues;
    }
}
