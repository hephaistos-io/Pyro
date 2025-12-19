package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.data.TemplateEntity;
import io.hephaistos.flagforge.data.TemplateSchema;
import io.hephaistos.flagforge.data.TemplateType;

import java.time.Instant;
import java.util.UUID;

public record TemplateResponse(UUID id, UUID applicationId, TemplateType type,
                               TemplateSchema schema, Instant createdAt, Instant updatedAt) {
    public static TemplateResponse fromEntity(TemplateEntity entity) {
        return new TemplateResponse(entity.getId(), entity.getApplicationId(), entity.getType(),
                entity.getSchema(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
