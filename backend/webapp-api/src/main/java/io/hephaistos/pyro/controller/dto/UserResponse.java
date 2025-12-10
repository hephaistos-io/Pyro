package io.hephaistos.pyro.controller.dto;

import io.hephaistos.pyro.data.UserEntity;

import java.util.Optional;
import java.util.UUID;

public record UserResponse(String firstName, String lastName, String email,
                           Optional<UUID> companyId) {

    public static UserResponse fromEntity(UserEntity userEntity) {
        return new UserResponse(userEntity.getFirstName(), userEntity.getLastName(),
                userEntity.getEmail(), userEntity.getCompanyId());
    }
}
