package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.data.ApplicationEntity;

import java.util.List;
import java.util.UUID;

public record ApplicationListResponse(UUID id, String name, UUID companyId,
                                      List<EnvironmentResponse> environments) {

    public static ApplicationListResponse fromEntity(ApplicationEntity applicationEntity) {
        List<EnvironmentResponse> environments = applicationEntity.getEnvironments()
                .stream()
                .map(EnvironmentResponse::fromEntity)
                .toList();
        return new ApplicationListResponse(applicationEntity.getId(), applicationEntity.getName(),
                applicationEntity.getCompanyId(), environments);
    }
}
