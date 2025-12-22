package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.data.CompanyEntity;

import java.util.UUID;

public record CompanyResponse(UUID id, String name) {

    public static CompanyResponse fromEntity(CompanyEntity companyEntity) {
        return new CompanyResponse(companyEntity.getId(), companyEntity.getName());
    }
}
