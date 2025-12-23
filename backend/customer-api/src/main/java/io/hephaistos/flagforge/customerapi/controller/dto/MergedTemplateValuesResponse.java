package io.hephaistos.flagforge.customerapi.controller.dto;

import io.hephaistos.flagforge.common.enums.TemplateType;
import io.hephaistos.flagforge.common.types.TemplateSchema;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Response containing merged template values with schema defaults and applied override.
 *
 * @param type              The template type (SYSTEM)
 * @param schema            The template schema definition
 * @param values            The merged values (defaults + override if applied)
 * @param appliedIdentifier The identifier whose override was applied, or null if none
 */
public record MergedTemplateValuesResponse(TemplateType type, TemplateSchema schema,
                                           Map<String, Object> values,
                                           @Nullable String appliedIdentifier) {
}
