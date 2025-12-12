package io.hephaistos.flagforge.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record UserRegistrationRequest(@Schema(requiredMode = REQUIRED) @NotBlank(
        message = "First name can't be blank") String firstName,
                                      @Schema(requiredMode = REQUIRED) @NotBlank(
                                              message = "Last name can't be blank") String lastName,
                                      @Schema(example = "your@email.com",
                                              requiredMode = REQUIRED) @NotBlank(
                                              message = "E-Mail can't be blank") @Email String email,
                                      @Schema(requiredMode = REQUIRED, minLength = 8) @NotBlank(
                                              message = "Password can't be blank") @Size(min = 8,
                                              message = "Password must be at least 8 characters") String password) {

    @Override
    public @NotNull String toString() {
        return "UserRegistrationRequest[firstName=" + firstName + ", lastName=" + lastName + ", email=" + email + "]";
    }
}
