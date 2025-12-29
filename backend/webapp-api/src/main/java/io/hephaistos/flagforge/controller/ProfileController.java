package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.controller.dto.CustomerResponse;
import io.hephaistos.flagforge.controller.dto.EmailChangeRequestDto;
import io.hephaistos.flagforge.controller.dto.ProfileUpdateRequest;
import io.hephaistos.flagforge.service.CustomerService;
import io.hephaistos.flagforge.service.EmailChangeService;
import io.hephaistos.flagforge.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/v1/profile")
@Tag(name = "profile")
public class ProfileController {

    private final CustomerService customerService;
    private final PasswordResetService passwordResetService;
    private final EmailChangeService emailChangeService;

    public ProfileController(CustomerService customerService,
            PasswordResetService passwordResetService, EmailChangeService emailChangeService) {
        this.customerService = customerService;
        this.passwordResetService = passwordResetService;
        this.emailChangeService = emailChangeService;
    }

    @Operation(summary = "Update own profile (firstName, lastName)")
    @PutMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public CustomerResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        var customer = customerService.updateOwnProfile(request);
        return CustomerResponse.fromEntity(customer);
    }

    @Operation(summary = "Request email change - sends verification email to new address")
    @PostMapping(value = "/email-change", consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestEmailChange(@Valid @RequestBody EmailChangeRequestDto request,
            HttpServletRequest httpRequest) {
        String baseUrl = getBaseUrl(httpRequest);
        emailChangeService.requestEmailChange(request.newEmail(), baseUrl);
    }

    @Operation(
            summary = "Request password reset from profile (authenticated) - sends email with reset link")
    @PostMapping(value = "/password-reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestPasswordResetAuthenticated(HttpServletRequest httpRequest) {
        String baseUrl = getBaseUrl(httpRequest);
        passwordResetService.requestPasswordResetAuthenticated(baseUrl);
    }

    private String getBaseUrl(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        return origin != null ? origin : request.getHeader("Referer");
    }
}
