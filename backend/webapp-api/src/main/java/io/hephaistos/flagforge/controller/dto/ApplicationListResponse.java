package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.data.ApplicationEntity;
import io.hephaistos.flagforge.service.AuditInfoService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for listing applications.
 * Includes optional audit info (createdAt, updatedAt, createdByName, updatedByName)
 * which is only populated for users with DEV or ADMIN role.
 */
public record ApplicationListResponse(
        UUID id,
        String name,
        UUID companyId,
        List<EnvironmentResponse> environments,
        Instant createdAt,
        Instant updatedAt,
        String createdByName,
        String updatedByName
) {

    /**
     * Creates response from entity without audit info (for backward compatibility).
     */
    public static ApplicationListResponse fromEntity(ApplicationEntity applicationEntity) {
        List<EnvironmentResponse> environments = applicationEntity.getEnvironments()
                .stream()
                .map(EnvironmentResponse::fromEntity)
                .toList();
        return new ApplicationListResponse(
                applicationEntity.getId(),
                applicationEntity.getName(),
                applicationEntity.getCompanyId(),
                environments,
                null, null, null, null
        );
    }

    /**
     * Creates response from entity with audit info based on user role.
     */
    public static ApplicationListResponse fromEntity(
            ApplicationEntity applicationEntity,
            AuditInfoService auditInfoService
    ) {
        List<EnvironmentResponse> environments = applicationEntity.getEnvironments()
                .stream()
                .map(env -> EnvironmentResponse.fromEntity(env, auditInfoService))
                .toList();

        return new ApplicationListResponse(
                applicationEntity.getId(),
                applicationEntity.getName(),
                applicationEntity.getCompanyId(),
                environments,
                auditInfoService.getCreatedAtIfAllowed(applicationEntity),
                auditInfoService.getUpdatedAtIfAllowed(applicationEntity),
                auditInfoService.getCreatedByNameIfAllowed(applicationEntity),
                auditInfoService.getUpdatedByNameIfAllowed(applicationEntity)
        );
    }
}
