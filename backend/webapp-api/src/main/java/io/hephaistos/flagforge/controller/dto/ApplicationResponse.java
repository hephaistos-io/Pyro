package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.data.ApplicationEntity;

import java.util.UUID;

public record ApplicationResponse(UUID id, String name, UUID companyId) {

    public static ApplicationResponse fromEntity(ApplicationEntity applicationEntity) {
        return new ApplicationResponse(applicationEntity.getId(), applicationEntity.getName(),
                applicationEntity.getCompanyId());
    }
}
