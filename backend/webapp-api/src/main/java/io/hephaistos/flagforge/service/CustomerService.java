package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.CustomerEntity;
import io.hephaistos.flagforge.controller.dto.CustomerRegistrationRequest;

import java.util.List;
import java.util.Optional;

public interface CustomerService {

    void registerCustomer(CustomerRegistrationRequest customerRegistrationRequest);

    Optional<CustomerEntity> getCustomerByEmail(String email);

    List<CustomerEntity> getAllCustomers();
}
