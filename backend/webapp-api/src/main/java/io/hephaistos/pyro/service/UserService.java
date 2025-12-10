package io.hephaistos.pyro.service;

import io.hephaistos.pyro.controller.dto.UserRegistrationRequest;
import io.hephaistos.pyro.data.UserEntity;

import java.util.Optional;

public interface UserService {

    void registerUser(UserRegistrationRequest userRegistrationRequest);

    Optional<UserEntity> getUserByEmail(String email);

    UserEntity getUserByEmailOrThrow(String email);

}
