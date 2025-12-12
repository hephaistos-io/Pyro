package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.data.CustomerEntity;

import java.util.Optional;
import java.util.UUID;

public record CustomerResponse(String firstName, String lastName, String email,
                               Optional<UUID> companyId) {

    public static CustomerResponse fromEntity(CustomerEntity customerEntity) {
        return new CustomerResponse(customerEntity.getFirstName(), customerEntity.getLastName(),
                customerEntity.getEmail(), customerEntity.getCompanyId());
    }
}
