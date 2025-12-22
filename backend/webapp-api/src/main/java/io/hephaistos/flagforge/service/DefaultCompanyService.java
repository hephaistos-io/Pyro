package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.CompanyEntity;
import io.hephaistos.flagforge.controller.dto.CompanyCreationRequest;
import io.hephaistos.flagforge.controller.dto.CompanyResponse;
import io.hephaistos.flagforge.data.repository.CompanyRepository;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.exception.CompanyAlreadyAssignedException;
import io.hephaistos.flagforge.security.FlagForgeSecurityContext;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class DefaultCompanyService implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CustomerRepository customerRepository;

    public DefaultCompanyService(CompanyRepository companyRepository,
            CustomerRepository customerRepository) {
        this.companyRepository = companyRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    public Optional<CompanyEntity> getCompanyForCurrentCustomer() {
        var securityContext = FlagForgeSecurityContext.getCurrent();
        return securityContext.getCompanyId().flatMap(companyRepository::findById);
    }

    @Override
    public Optional<CompanyEntity> getCompany(UUID companyId) {
        return companyRepository.findById(companyId);
    }

    @Override
    public CompanyResponse createCompanyForCurrentCustomer(
            CompanyCreationRequest companyCreationRequest) {
        var securityContext = FlagForgeSecurityContext.getCurrent();

        // Check security context first to avoid unnecessary customer lookup
        if (securityContext.getCompanyId().isPresent()) {
            throw new CompanyAlreadyAssignedException(
                    "Can't create company, the customer already has one assigned!");
        }

        var customer = customerRepository.findByEmail(securityContext.getCustomerName())
                .orElseThrow(() -> new UsernameNotFoundException("Couldn't find customer!"));

        var company = new CompanyEntity();
        company.setName(companyCreationRequest.companyName());
        companyRepository.save(company);
        customer.setCompanyId(company.getId());

        // Update the security context's cached companyId
        securityContext.setCompanyId(company.getId());

        return CompanyResponse.fromEntity(company);
    }
}
