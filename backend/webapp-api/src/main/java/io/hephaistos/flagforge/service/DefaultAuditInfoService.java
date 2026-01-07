package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.AuditableEntity;
import io.hephaistos.flagforge.common.data.CustomerEntity;
import io.hephaistos.flagforge.common.enums.CustomerRole;
import io.hephaistos.flagforge.common.security.FlagForgeSecurityContext;
import io.hephaistos.flagforge.controller.dto.AuditInfo;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of AuditInfoService that resolves user IDs to display names
 * and enforces role-based access to audit information.
 */
@Service
@Transactional(readOnly = true)
public class DefaultAuditInfoService implements AuditInfoService {

    private final CustomerRepository customerRepository;

    public DefaultAuditInfoService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public boolean canViewAuditInfo() {
        try {
            var securityContext = FlagForgeSecurityContext.getCurrent();
            CustomerRole role = securityContext.getRole();
            return role == CustomerRole.ADMIN || role == CustomerRole.DEV;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String resolveUserName(UUID userId) {
        if (userId == null || userId.equals(AuditableEntity.SYSTEM_USER_ID)) {
            return "System";
        }

        return customerRepository.findById(userId)
                .map(this::formatUserName)
                .orElse("Unknown");
    }

    @Override
    public AuditInfo createAuditInfo(AuditableEntity entity) {
        if (!canViewAuditInfo() || entity == null) {
            return null;
        }

        return new AuditInfo(
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                resolveUserName(entity.getCreatedBy()),
                resolveUserName(entity.getUpdatedBy())
        );
    }

    @Override
    public Instant getCreatedAtIfAllowed(AuditableEntity entity) {
        if (!canViewAuditInfo() || entity == null) {
            return null;
        }
        return entity.getCreatedAt();
    }

    @Override
    public Instant getUpdatedAtIfAllowed(AuditableEntity entity) {
        if (!canViewAuditInfo() || entity == null) {
            return null;
        }
        return entity.getUpdatedAt();
    }

    @Override
    public String getCreatedByNameIfAllowed(AuditableEntity entity) {
        if (!canViewAuditInfo() || entity == null) {
            return null;
        }
        return resolveUserName(entity.getCreatedBy());
    }

    @Override
    public String getUpdatedByNameIfAllowed(AuditableEntity entity) {
        if (!canViewAuditInfo() || entity == null) {
            return null;
        }
        return resolveUserName(entity.getUpdatedBy());
    }

    private String formatUserName(CustomerEntity customer) {
        String firstName = Optional.ofNullable(customer.getFirstName()).orElse("");
        String lastName = Optional.ofNullable(customer.getLastName()).orElse("");

        if (firstName.isEmpty() && lastName.isEmpty()) {
            return customer.getEmail();
        }

        return (firstName + " " + lastName).trim();
    }
}
