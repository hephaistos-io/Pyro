package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.UserRegistrationRequest;
import io.hephaistos.flagforge.data.CustomerEntity;

import java.util.Optional;

public interface UserService {

    void registerUser(UserRegistrationRequest userRegistrationRequest);

    Optional<CustomerEntity> getUserByEmail(String email);

    CustomerEntity getUserByEmailOrThrow(String email);

}
