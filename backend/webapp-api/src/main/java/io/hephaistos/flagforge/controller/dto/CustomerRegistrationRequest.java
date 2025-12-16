package io.hephaistos.flagforge.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record CustomerRegistrationRequest(@Schema(requiredMode = REQUIRED) @NotBlank(
        message = "First name can't be blank") String firstName,

                                          @Schema(requiredMode = REQUIRED) @NotBlank(
                                                  message = "Last name can't be blank") String lastName,

                                          @Schema(example = "your@email.com",
                                                  requiredMode = NOT_REQUIRED,
                                                  description = "Required unless inviteToken is provided") @Email String email,

                                          @Schema(requiredMode = REQUIRED, minLength = 8) @NotBlank(
                                                  message = "Password can't be blank") @Size(
                                                  min = 8,
                                                  message = "Password must be at least 8 characters") String password,

                                          @Schema(requiredMode = NOT_REQUIRED,
                                                  description = "Invite token for joining an existing company") String inviteToken) {
    /**
     * Creates a registration request without an invite token (standard registration).
     */
    public static CustomerRegistrationRequest withEmail(String firstName, String lastName,
            String email, String password) {
        return new CustomerRegistrationRequest(firstName, lastName, email, password, null);
    }

    /**
     * Creates a registration request with an invite token (invited user registration).
     */
    public static CustomerRegistrationRequest withInvite(String firstName, String lastName,
            String password, String inviteToken) {
        return new CustomerRegistrationRequest(firstName, lastName, null, password, inviteToken);
    }

    @Override
    public @NotNull String toString() {
        return "CustomerRegistrationRequest[firstName=" + firstName + ", lastName=" + lastName + ", email=" + email + ", inviteToken=" + (
                inviteToken != null ?
                        "[PRESENT]" :
                        "[ABSENT]") + "]";
    }
}
