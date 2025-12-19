package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.data.TemplateSchema;
import io.hephaistos.flagforge.data.TemplateType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MergedTemplateValuesResponse(UUID applicationId, UUID environmentId,
                                           TemplateType type, TemplateSchema schema,
                                           Map<String, Object> values,
                                           List<String> appliedIdentifiers) {
}
