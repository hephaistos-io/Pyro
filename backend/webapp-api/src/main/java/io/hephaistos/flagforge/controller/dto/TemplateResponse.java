package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.data.TemplateEntity;
import io.hephaistos.flagforge.common.enums.TemplateType;
import io.hephaistos.flagforge.common.types.TemplateSchema;

import java.time.Instant;
import java.util.UUID;

public record TemplateResponse(UUID id, UUID applicationId, TemplateType type,
                               TemplateSchema schema, Instant createdAt, Instant updatedAt) {
    public static TemplateResponse fromEntity(TemplateEntity entity) {
        return new TemplateResponse(entity.getId(), entity.getApplicationId(), entity.getType(),
                entity.getSchema(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
