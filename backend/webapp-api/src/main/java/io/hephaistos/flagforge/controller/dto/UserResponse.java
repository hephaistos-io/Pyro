package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.data.CustomerEntity;

import java.util.Optional;
import java.util.UUID;

public record UserResponse(String firstName, String lastName, String email,
                           Optional<UUID> companyId) {

    public static UserResponse fromEntity(CustomerEntity customerEntity) {
        return new UserResponse(customerEntity.getFirstName(), customerEntity.getLastName(),
                customerEntity.getEmail(), customerEntity.getCompanyId());
    }
}
