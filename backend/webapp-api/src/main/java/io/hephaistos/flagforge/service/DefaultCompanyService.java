package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.ApplicationEntity;
import io.hephaistos.flagforge.common.data.CompanyEntity;
import io.hephaistos.flagforge.common.data.EnvironmentEntity;
import io.hephaistos.flagforge.controller.dto.CompanyCreationRequest;
import io.hephaistos.flagforge.controller.dto.CompanyResponse;
import io.hephaistos.flagforge.controller.dto.CompanyStatisticsResponse;
import io.hephaistos.flagforge.controller.dto.CompanyStatisticsResponse.ApplicationStatistics;
import io.hephaistos.flagforge.controller.dto.CompanyStatisticsResponse.EnvironmentStatistics;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.data.repository.CompanyRepository;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.exception.CompanyAlreadyAssignedException;
import io.hephaistos.flagforge.exception.NoCompanyAssignedException;
import io.hephaistos.flagforge.security.FlagForgeSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class DefaultCompanyService implements CompanyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCompanyService.class);

    private final CompanyRepository companyRepository;
    private final CustomerRepository customerRepository;
    private final ApplicationRepository applicationRepository;

    public DefaultCompanyService(CompanyRepository companyRepository,
            CustomerRepository customerRepository, ApplicationRepository applicationRepository) {
        this.companyRepository = companyRepository;
        this.customerRepository = customerRepository;
        this.applicationRepository = applicationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CompanyEntity> getCompanyForCurrentCustomer() {
        var securityContext = FlagForgeSecurityContext.getCurrent();
        return securityContext.getCompanyId().flatMap(companyRepository::findById);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CompanyEntity> getCompany(UUID companyId) {
        return companyRepository.findById(companyId);
    }

    @Override
    public CompanyResponse createCompanyForCurrentCustomer(
            CompanyCreationRequest companyCreationRequest) {
        var securityContext = FlagForgeSecurityContext.getCurrent();
        LOGGER.info("Creating company '{}' for customer: {}", companyCreationRequest.companyName(),
                securityContext.getCustomerName());

        // Check security context first to avoid unnecessary customer lookup
        if (securityContext.getCompanyId().isPresent()) {
            LOGGER.warn("Customer {} already has a company assigned",
                    securityContext.getCustomerName());
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

        LOGGER.info("Created company '{}' with ID: {}", company.getName(), company.getId());
        return CompanyResponse.fromEntity(company);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyStatisticsResponse getCompanyStatistics() {
        var securityContext = FlagForgeSecurityContext.getCurrent();
        UUID companyId = securityContext.getCompanyId()
                .orElseThrow(
                        () -> new NoCompanyAssignedException("Customer has no company assigned!"));

        List<ApplicationEntity> applications = applicationRepository.findByCompanyId(companyId);

        List<ApplicationStatistics> applicationStats =
                applications.stream().map(this::mapToApplicationStatistics).toList();

        int totalMonthlyPrice = applicationStats.stream()
                .mapToInt(ApplicationStatistics::totalMonthlyPriceUsd)
                .sum();

        return new CompanyStatisticsResponse(applicationStats, totalMonthlyPrice);
    }

    private ApplicationStatistics mapToApplicationStatistics(ApplicationEntity application) {
        List<EnvironmentStatistics> environmentStats = application.getEnvironments()
                .stream()
                .map(this::mapToEnvironmentStatistics)
                .toList();

        int totalMonthlyPrice =
                environmentStats.stream().mapToInt(EnvironmentStatistics::monthlyPriceUsd).sum();

        return new ApplicationStatistics(application.getId(), application.getName(),
                environmentStats, totalMonthlyPrice);
    }

    private EnvironmentStatistics mapToEnvironmentStatistics(EnvironmentEntity environment) {
        return new EnvironmentStatistics(environment.getId(), environment.getName(),
                environment.getDescription(), environment.getTier().name(),
                environment.getTier().getMonthlyPriceUsd());
    }
}
