package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.CompanyCreationRequest;
import io.hephaistos.flagforge.controller.dto.CompanyResponse;
import io.hephaistos.flagforge.data.CompanyEntity;
import io.hephaistos.flagforge.data.repository.CompanyRepository;
import io.hephaistos.flagforge.data.repository.UserRepository;
import io.hephaistos.flagforge.exception.CompanyAlreadyAssignedException;
import io.hephaistos.flagforge.security.FlagForgeSecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class DefaultCompanyService implements CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public DefaultCompanyService(CompanyRepository companyRepository,
            UserRepository userRepository) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<CompanyEntity> getCompanyForCurrentUser() {
        var pyroSecurityContext = (FlagForgeSecurityContext) SecurityContextHolder.getContext();
        return pyroSecurityContext.getCompanyId().flatMap(companyRepository::findById);
    }

    @Override
    public Optional<CompanyEntity> getCompany(UUID companyId) {
        return companyRepository.findById(companyId);
    }

    @Override
    public CompanyResponse createCompanyForCurrentUser(
            CompanyCreationRequest companyCreationRequest) {
        var pyroSecurityContext = (FlagForgeSecurityContext) SecurityContextHolder.getContext();

        // Check security context first to avoid unnecessary user lookup
        if (pyroSecurityContext.getCompanyId().isPresent()) {
            throw new CompanyAlreadyAssignedException(
                    "Can't create company, the user already has one assigned!");
        }

        var user = userRepository.findByEmail(pyroSecurityContext.getUserName())
                .orElseThrow(() -> new UsernameNotFoundException("Couldnt find user!"));

        var company = new CompanyEntity();
        company.setName(companyCreationRequest.companyName());
        companyRepository.save(company);
        user.setCompanyId(company.getId());
        return CompanyResponse.fromEntity(company);
    }
}
