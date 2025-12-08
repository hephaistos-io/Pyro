package io.hephaistos.pyro.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record UserRegistrationRequest(@Schema(requiredMode = REQUIRED) @NotBlank(
        message = "First name can't be blank") String firstName,
                                      @Schema(requiredMode = REQUIRED) @NotBlank(
                                              message = "Last name can't be blank") String lastName,
                                      @Schema(example = "your@email.com",
                                              requiredMode = REQUIRED) @NotBlank(
                                              message = "E-Mail can't be blank") @Email String email,
                                      @Schema(requiredMode = REQUIRED) @NotBlank(
                                              message = "password can't be blank") String password) {

    @Override
    public @NotNull String toString() {
        return "UserRegistrationRequest[firstName=" + firstName + ", lastName=" + lastName + ", email=" + email + "]";
    }
}
