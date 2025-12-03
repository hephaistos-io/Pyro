package io.hephaistos.pyro.controller;

import io.hephaistos.pyro.controller.dto.UserResponse;
import io.hephaistos.pyro.security.PyroSecurityContext;
import io.hephaistos.pyro.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/v1/user")
@Tag(name = "user")
@Tag(name = "v1")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Retrieve profile of user")
    @GetMapping(value = "/profile", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public UserResponse profile() {
        var pyroSecurityContext = (PyroSecurityContext) SecurityContextHolder.getContext();
        return userService.getUserByEmail(pyroSecurityContext.getUserName())
                .map(UserResponse::fromEntity)
                .orElseThrow(
                        () -> new UsernameNotFoundException("User not found for your session"));
    }
}
