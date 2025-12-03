package io.hephaistos.pyro.service;

import io.hephaistos.pyro.controller.dto.UserRegistrationRequest;
import io.hephaistos.pyro.data.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void beforeEach() {
        userRepository.deleteAll();
    }

    @Test
    @Tag("integration")
    void newUsersArePersisted() {
        userService.registerUser(newUserRegistrationRequest());
        assertThat(userRepository.findAll()).hasSize(1);
    }

    @Test
    @Tag("integration")
    void multipleUsersArePersistedIfEmailIsDifferent() {
        userService.registerUser(newUserRegistrationRequest());
        userService.registerUser(newUserRegistrationRequest("different@mail.com"));
        userService.registerUser(newUserRegistrationRequest("another@mail.com"));
        assertThat(userRepository.findAll()).hasSize(3);
    }

    @Test
    @Tag("integration")
    void newUserWithExistingEmailThrowsException() {
        userService.registerUser(newUserRegistrationRequest());
        assertThatThrownBy(() -> userService.registerUser(newUserRegistrationRequest()),
                "Email already exists");
    }


    private UserRegistrationRequest newUserRegistrationRequest() {
        return newUserRegistrationRequest("name@domain.com");
    }

    private UserRegistrationRequest newUserRegistrationRequest(String email) {
        return new UserRegistrationRequest("FirstName", "LastName", email, "123456");
    }

}
