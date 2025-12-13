package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.data.EnvironmentEntity;
import io.hephaistos.flagforge.data.EnvironmentTier;

import java.util.UUID;

public record EnvironmentResponse(UUID id, UUID applicationId, String name, String description,
                                  EnvironmentTier tier) {

    public static EnvironmentResponse fromEntity(EnvironmentEntity environmentEntity) {
        return new EnvironmentResponse(environmentEntity.getId(),
                environmentEntity.getApplicationId(), environmentEntity.getName(),
                environmentEntity.getDescription(), environmentEntity.getTier());
    }
}
