package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.data.TemplateType;
import io.hephaistos.flagforge.data.TemplateValuesEntity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TemplateValuesResponse(UUID id, UUID applicationId, UUID environmentId,
                                     TemplateType type, String identifier,
                                     Map<String, Object> values, Instant createdAt,
                                     Instant updatedAt) {
    public static TemplateValuesResponse fromEntity(TemplateValuesEntity entity) {
        return new TemplateValuesResponse(entity.getId(), entity.getApplicationId(),
                entity.getEnvironmentId(), entity.getType(), entity.getIdentifier(),
                entity.getValues(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
