package io.hephaistos.pyro.controller.dto;

import io.hephaistos.pyro.data.CompanyEntity;

import java.util.UUID;

public record CompanyResponse(UUID id, String name) {

    public static CompanyResponse fromEntity(CompanyEntity companyEntity) {
        return new CompanyResponse(companyEntity.getId(), companyEntity.getName());
    }
}
