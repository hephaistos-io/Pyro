package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.CompanyEntity;
import io.hephaistos.flagforge.controller.dto.CompanyCreationRequest;
import io.hephaistos.flagforge.controller.dto.CompanyResponse;

import java.util.Optional;
import java.util.UUID;

public interface CompanyService {

    Optional<CompanyEntity> getCompanyForCurrentCustomer();

    Optional<CompanyEntity> getCompany(UUID companyId);

    CompanyResponse createCompanyForCurrentCustomer(CompanyCreationRequest companyCreationRequest);
}
