package io.hephaistos.pyro.service;

import io.hephaistos.pyro.controller.dto.UserRegistrationRequest;
import io.hephaistos.pyro.data.UserEntity;
import io.hephaistos.pyro.data.repository.UserRepository;
import io.hephaistos.pyro.exception.BreachedPasswordException;
import io.hephaistos.pyro.exception.DuplicateResourceException;
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
public class DefaultUserService implements UserService, UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final BreachedPasswordService breachedPasswordService;

    public DefaultUserService(PasswordEncoder passwordEncoder, UserRepository userRepository,
            BreachedPasswordService breachedPasswordService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.breachedPasswordService = breachedPasswordService;
    }

    @Override
    public void registerUser(UserRegistrationRequest userRegistrationRequest) {
        if (getUserByEmail(userRegistrationRequest.email()).isPresent()) {
            throw new DuplicateResourceException("Email already exists");
        }

        // Check if password has been breached
        if (breachedPasswordService.isPasswordBreached(userRegistrationRequest.password())) {
            throw new BreachedPasswordException(
                    "This password has been found in data breaches. Please choose a different password.");
        }

        var userEntity = new UserEntity();
        userEntity.setEmail(userRegistrationRequest.email());
        userEntity.setFirstName(userRegistrationRequest.firstName());
        userEntity.setLastName(userRegistrationRequest.lastName());
        userEntity.setPassword(passwordEncoder.encode(userRegistrationRequest.password()));

        try {
            userRepository.save(userEntity);
        }
        catch (DataIntegrityViolationException ex) {
            // Handle race condition: another thread saved the same email between our check and save
            throw new DuplicateResourceException("Email already exists", ex);
        }
    }

    @Override
    public Optional<UserEntity> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public UserEntity getUserByEmailOrThrow(String email) {
        return getUserByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));
    }

    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return getUserByEmail(username).map(user -> User.builder()
                        .username(user.getEmail())
                        .password(user.getPassword())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username: " + username));
    }
}
