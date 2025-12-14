package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.CustomerRegistrationRequest;
import io.hephaistos.flagforge.data.ApplicationEntity;
import io.hephaistos.flagforge.data.CustomerEntity;
import io.hephaistos.flagforge.data.CustomerRole;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.exception.BreachedPasswordException;
import io.hephaistos.flagforge.exception.DuplicateResourceException;
import io.hephaistos.flagforge.security.FlagForgeUserDetails;
import org.jspecify.annotations.NullMarked;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public List<CustomerEntity> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return customerRepository.findByEmailWithAccessibleApplications(username)
                .map(this::toUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Customer not found with email: " + username));
    }

    private FlagForgeUserDetails toUserDetails(CustomerEntity customer) {
        var accessibleAppIds = customer.getAccessibleApplications()
                .stream()
                .map(ApplicationEntity::getId)
                .collect(Collectors.toSet());

        return new FlagForgeUserDetails(customer.getEmail(), customer.getPassword(),
                customer.getId(), customer.getCompanyId().orElse(null), accessibleAppIds,
                List.of(new SimpleGrantedAuthority(customer.getRole().name())));
    }
}
