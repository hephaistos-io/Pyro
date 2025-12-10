package io.hephaistos.pyro.service;

import io.hephaistos.pyro.controller.dto.CompanyCreationRequest;
import io.hephaistos.pyro.controller.dto.CompanyResponse;
import io.hephaistos.pyro.data.CompanyEntity;

import java.util.Optional;
import java.util.UUID;

public interface CompanyService {

    Optional<CompanyEntity> getCompanyForCurrentUser();

    Optional<CompanyEntity> getCompany(UUID companyId);

    CompanyResponse createCompanyForCurrentUser(CompanyCreationRequest companyCreationRequest);
}
