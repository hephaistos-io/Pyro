package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.CompanyCreationRequest;
import io.hephaistos.flagforge.controller.dto.CompanyResponse;
import io.hephaistos.flagforge.data.CompanyEntity;

import java.util.Optional;
import java.util.UUID;

public interface CompanyService {

    Optional<CompanyEntity> getCompanyForCurrentUser();

    Optional<CompanyEntity> getCompany(UUID companyId);

    CompanyResponse createCompanyForCurrentUser(CompanyCreationRequest companyCreationRequest);
}
