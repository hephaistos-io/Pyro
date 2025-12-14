package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.CustomerRegistrationRequest;
import io.hephaistos.flagforge.data.CustomerEntity;
import io.hephaistos.flagforge.data.CustomerRole;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.exception.BreachedPasswordException;
import io.hephaistos.flagforge.exception.DuplicateResourceException;
import org.jspecify.annotations.NullMarked;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class DefaultCustomerService implements CustomerService, UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final CustomerRepository customerRepository;
    private final BreachedPasswordService breachedPasswordService;

    public DefaultCustomerService(PasswordEncoder passwordEncoder,
            CustomerRepository customerRepository,
            BreachedPasswordService breachedPasswordService) {
        this.passwordEncoder = passwordEncoder;
        this.customerRepository = customerRepository;
        this.breachedPasswordService = breachedPasswordService;
    }

    @Override
    public void registerCustomer(CustomerRegistrationRequest customerRegistrationRequest) {
        if (getCustomerByEmail(customerRegistrationRequest.email()).isPresent()) {
            throw new DuplicateResourceException("Email already exists");
        }

        // Check if password has been breached
        if (breachedPasswordService.isPasswordBreached(customerRegistrationRequest.password())) {
            throw new BreachedPasswordException(
                    "This password has been found in data breaches. Please choose a different password.");
        }

        var customer = new CustomerEntity();
        customer.setEmail(customerRegistrationRequest.email());
        customer.setFirstName(customerRegistrationRequest.firstName());
        customer.setLastName(customerRegistrationRequest.lastName());
        customer.setPassword(passwordEncoder.encode(customerRegistrationRequest.password()));
        customer.setRole(CustomerRole.ADMIN);

        try {
            customerRepository.save(customer);
        }
        catch (DataIntegrityViolationException ex) {
            // Handle race condition: another thread saved the same email between our check and save
            throw new DuplicateResourceException("Email already exists", ex);
        }
    }

    @Override
    public Optional<CustomerEntity> getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    @Override
    public CustomerEntity getCustomerByEmailOrThrow(String email) {
        return getCustomerByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));
    }

    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return getCustomerByEmail(username).map(customer -> User.builder()
                        .username(customer.getEmail())
                        .password(customer.getPassword())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Customer not found with email: " + username));
    }
}
