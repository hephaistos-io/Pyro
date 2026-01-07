package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.data.ApplicationEntity;
import io.hephaistos.flagforge.service.AuditInfoService;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for application details.
 * Includes optional AuditInfo which is only populated for users with DEV or ADMIN role.
 */
public record ApplicationResponse(
        UUID id,
        String name,
        UUID companyId,
        List<EnvironmentResponse> environments,
        List<TemplateResponse> templates,
        AuditInfo audit
) {

    /**
     * Creates response from entity without audit info (for backward compatibility).
     */
    public static ApplicationResponse fromEntity(ApplicationEntity applicationEntity) {
        List<EnvironmentResponse> environments = applicationEntity.getEnvironments()
                .stream()
                .map(EnvironmentResponse::fromEntity)
                .toList();
        List<TemplateResponse> templates = applicationEntity.getTemplates()
                .stream()
                .map(TemplateResponse::fromEntity)
                .toList();
        return new ApplicationResponse(
                applicationEntity.getId(),
                applicationEntity.getName(),
                applicationEntity.getCompanyId(),
                environments,
                templates,
                null
        );
    }

    /**
     * Creates response from entity with audit info based on user role.
     */
    public static ApplicationResponse fromEntity(
            ApplicationEntity applicationEntity,
            AuditInfoService auditInfoService
    ) {
        List<EnvironmentResponse> environments = applicationEntity.getEnvironments()
                .stream()
                .map(env -> EnvironmentResponse.fromEntity(env, auditInfoService))
                .toList();
        List<TemplateResponse> templates = applicationEntity.getTemplates()
                .stream()
                .map(TemplateResponse::fromEntity)
                .toList();

        return new ApplicationResponse(
                applicationEntity.getId(),
                applicationEntity.getName(),
                applicationEntity.getCompanyId(),
                environments,
                templates,
                auditInfoService.createAuditInfo(applicationEntity)
        );
    }
}
