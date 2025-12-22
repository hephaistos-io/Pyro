package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.ApplicationEntity;
import io.hephaistos.flagforge.common.data.CompanyInviteEntity;
import io.hephaistos.flagforge.common.data.CustomerEntity;
import io.hephaistos.flagforge.common.enums.CustomerRole;
import io.hephaistos.flagforge.controller.dto.CustomerRegistrationRequest;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.exception.BreachedPasswordException;
import io.hephaistos.flagforge.exception.DuplicateResourceException;
import io.hephaistos.flagforge.security.FlagForgeUserDetails;
import io.hephaistos.flagforge.security.RequireAdmin;
import org.jspecify.annotations.NullMarked;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@Transactional
public class DefaultCustomerService implements CustomerService, UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final CustomerRepository customerRepository;
    private final BreachedPasswordService breachedPasswordService;
    private final InviteService inviteService;

    public DefaultCustomerService(PasswordEncoder passwordEncoder,
            CustomerRepository customerRepository, BreachedPasswordService breachedPasswordService,
            InviteService inviteService) {
        this.passwordEncoder = passwordEncoder;
        this.customerRepository = customerRepository;
        this.breachedPasswordService = breachedPasswordService;
        this.inviteService = inviteService;
    }

    @Override
    public void registerCustomer(CustomerRegistrationRequest request) {
        if (isNotBlank(request.inviteToken())) {
            registerWithInvite(request);
        }
        else {
            registerWithoutInvite(request);
        }
    }

    private void registerWithInvite(CustomerRegistrationRequest request) {
        // Validate and get the invite
        CompanyInviteEntity invite = inviteService.getInviteByToken(request.inviteToken());

        // Check if email is already registered
        if (getCustomerByEmail(invite.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Email already exists");
        }

        // Check if password has been breached
        if (breachedPasswordService.isPasswordBreached(request.password())) {
            throw new BreachedPasswordException(
                    "This password has been found in data breaches. Please choose a different password.");
        }

        // Create customer with values from invite
        var customer = new CustomerEntity();
        customer.setEmail(invite.getEmail());
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setPassword(passwordEncoder.encode(request.password()));
        customer.setCompanyId(invite.getCompanyId());
        customer.setRole(invite.getAssignedRole());
        customer.setAccessibleApplications(new HashSet<>(invite.getPreAssignedApplications()));

        try {
            customerRepository.save(customer);
        }
        catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Email already exists", ex);
        }

        // Mark invite as used
        inviteService.consumeInvite(invite, customer.getId());
    }

    private void registerWithoutInvite(CustomerRegistrationRequest request) {
        // Email is required for non-invite registration
        if (isBlank(request.email())) {
            throw new IllegalArgumentException("Email is required");
        }

        if (getCustomerByEmail(request.email()).isPresent()) {
            throw new DuplicateResourceException("Email already exists");
        }

        // Check if password has been breached
        if (breachedPasswordService.isPasswordBreached(request.password())) {
            throw new BreachedPasswordException(
                    "This password has been found in data breaches. Please choose a different password.");
        }

        var customer = new CustomerEntity();
        customer.setEmail(request.email());
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setPassword(passwordEncoder.encode(request.password()));
        customer.setRole(CustomerRole.ADMIN);

        try {
            customerRepository.save(customer);
        }
        catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Email already exists", ex);
        }
    }

    @Override
    public Optional<CustomerEntity> getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    @Override
    @RequireAdmin
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
                List.of(new SimpleGrantedAuthority(customer.getRole().toAuthority())));
    }
}
