package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.enums.TemplateType;
import io.hephaistos.flagforge.common.types.TemplateSchema;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MergedTemplateValuesResponse(UUID applicationId, UUID environmentId,
                                           TemplateType type, TemplateSchema schema,
                                           Map<String, Object> values,
                                           List<String> appliedIdentifiers) {
}
