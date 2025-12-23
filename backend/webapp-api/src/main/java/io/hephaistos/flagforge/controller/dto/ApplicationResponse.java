package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.data.ApplicationEntity;

import java.util.List;
import java.util.UUID;

public record ApplicationResponse(UUID id, String name, UUID companyId,
                                  List<EnvironmentResponse> environments,
                                  List<TemplateResponse> templates) {

    public static ApplicationResponse fromEntity(ApplicationEntity applicationEntity) {
        List<EnvironmentResponse> environments = applicationEntity.getEnvironments()
                .stream().map(EnvironmentResponse::fromEntity).toList();
        List<TemplateResponse> templates =
                applicationEntity.getTemplates().stream().map(TemplateResponse::fromEntity)
                .toList();
        return new ApplicationResponse(applicationEntity.getId(), applicationEntity.getName(),
                applicationEntity.getCompanyId(), environments, templates);
    }
}
