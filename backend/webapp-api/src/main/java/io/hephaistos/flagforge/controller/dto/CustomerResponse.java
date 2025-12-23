package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.data.CustomerEntity;
import io.hephaistos.flagforge.common.enums.CustomerRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record CustomerResponse(UUID id, String firstName, String lastName, String email,
                               Optional<UUID> companyId, CustomerRole role,
                               List<ApplicationAccessResponse> applications) {

    public static CustomerResponse fromEntity(CustomerEntity customerEntity) {
        List<ApplicationAccessResponse> applications = customerEntity.getAccessibleApplications()
                .stream()
                .map(ApplicationAccessResponse::fromEntity)
                .toList();

        return new CustomerResponse(customerEntity.getId(), customerEntity.getFirstName(),
                customerEntity.getLastName(),
                customerEntity.getEmail(), customerEntity.getCompanyId(), customerEntity.getRole(),
                applications);
    }
}
