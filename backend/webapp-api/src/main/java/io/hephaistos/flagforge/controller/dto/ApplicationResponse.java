package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.data.ApplicationEntity;

import java.util.List;
import java.util.UUID;

public record ApplicationResponse(UUID id, String name, UUID companyId,
                                  List<EnvironmentResponse> environments) {

    public static ApplicationResponse fromEntity(ApplicationEntity applicationEntity) {
        List<EnvironmentResponse> environments = applicationEntity.getEnvironments()
                .stream()
                .map(EnvironmentResponse::fromEntity)
                .toList();
        return new ApplicationResponse(applicationEntity.getId(), applicationEntity.getName(),
                applicationEntity.getCompanyId(), environments);
    }
}
