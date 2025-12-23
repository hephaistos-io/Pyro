package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.data.ApplicationEntity;

import java.util.UUID;

public record ApplicationAccessResponse(UUID id, String name) {

    public static ApplicationAccessResponse fromEntity(ApplicationEntity applicationEntity) {
        return new ApplicationAccessResponse(applicationEntity.getId(),
                applicationEntity.getName());
    }
}
