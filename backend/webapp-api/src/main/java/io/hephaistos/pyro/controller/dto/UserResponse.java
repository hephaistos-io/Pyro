package io.hephaistos.pyro.controller.dto;

import io.hephaistos.pyro.data.UserEntity;

public record UserResponse(String firstName, String lastName, String email) {

    public static UserResponse fromEntity(UserEntity userEntity) {
        return new UserResponse(userEntity.getFirstName(), userEntity.getLastName(),
                userEntity.getEmail());
    }
}
