package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.CustomerEntity;
import io.hephaistos.flagforge.controller.dto.CustomerRegistrationRequest;
import io.hephaistos.flagforge.controller.dto.ProfileUpdateRequest;
import io.hephaistos.flagforge.controller.dto.UpdateCustomerRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerService {

    void registerCustomer(CustomerRegistrationRequest customerRegistrationRequest);

    Optional<CustomerEntity> getCustomerByEmail(String email);

    List<CustomerEntity> getAllCustomers();

    CustomerEntity updateCustomer(UUID customerId, UpdateCustomerRequest request);

    /**
     * Updates the current authenticated user's profile (firstName, lastName only).
     *
     * @param request the profile update request
     * @return the updated customer entity
     */
    CustomerEntity updateOwnProfile(ProfileUpdateRequest request);
}
