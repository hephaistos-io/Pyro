package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.enums.CustomerRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record InviteCreationRequest(
        @Schema(example = "newuser@example.com", requiredMode = REQUIRED) @NotBlank(
                message = "Email can't be blank") @Email String email,

        @Schema(requiredMode = REQUIRED) @NotNull(message = "Role is required") CustomerRole role,

        @Schema(description = "Application IDs to grant access to",
                requiredMode = NOT_REQUIRED) Set<UUID> applicationIds,

        @Schema(description = "Days until invite expires (default: 7)",
                requiredMode = NOT_REQUIRED) Integer expiresInDays) {
    public int getExpiresInDays() {
        return expiresInDays != null ? expiresInDays : 7;
    }
}
