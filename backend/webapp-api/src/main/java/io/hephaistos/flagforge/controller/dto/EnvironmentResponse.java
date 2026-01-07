package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.data.EnvironmentEntity;
import io.hephaistos.flagforge.common.enums.PricingTier;
import io.hephaistos.flagforge.service.AuditInfoService;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for environment information.
 * Includes optional audit info which is only populated for users with DEV or ADMIN role.
 */
public record EnvironmentResponse(
        UUID id,
        UUID applicationId,
        String name,
        String description,
        PricingTier tier,
        Instant createdAt,
        Instant updatedAt,
        String createdByName,
        String updatedByName
) {

    /**
     * Creates response from entity without audit info (for backward compatibility).
     */
    public static EnvironmentResponse fromEntity(EnvironmentEntity environmentEntity) {
        return new EnvironmentResponse(
                environmentEntity.getId(),
                environmentEntity.getApplicationId(),
                environmentEntity.getName(),
                environmentEntity.getDescription(),
                environmentEntity.getTier(),
                null, null, null, null
        );
    }

    /**
     * Creates response from entity with audit info based on user role.
     */
    public static EnvironmentResponse fromEntity(
            EnvironmentEntity environmentEntity,
            AuditInfoService auditInfoService
    ) {
        return new EnvironmentResponse(
                environmentEntity.getId(),
                environmentEntity.getApplicationId(),
                environmentEntity.getName(),
                environmentEntity.getDescription(),
                environmentEntity.getTier(),
                auditInfoService.getCreatedAtIfAllowed(environmentEntity),
                auditInfoService.getUpdatedAtIfAllowed(environmentEntity),
                auditInfoService.getCreatedByNameIfAllowed(environmentEntity),
                auditInfoService.getUpdatedByNameIfAllowed(environmentEntity)
        );
    }
}
