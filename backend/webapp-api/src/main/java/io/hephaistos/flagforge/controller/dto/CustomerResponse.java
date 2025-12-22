package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.data.CustomerEntity;
import io.hephaistos.flagforge.common.enums.CustomerRole;

import java.util.Optional;
import java.util.UUID;

public record CustomerResponse(String firstName, String lastName, String email,
                               Optional<UUID> companyId, CustomerRole role) {

    public static CustomerResponse fromEntity(CustomerEntity customerEntity) {
        return new CustomerResponse(customerEntity.getFirstName(), customerEntity.getLastName(),
                customerEntity.getEmail(), customerEntity.getCompanyId(), customerEntity.getRole());
    }
}
