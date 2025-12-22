package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.enums.KeyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record ApiKeyCreationRequest(@Schema(requiredMode = REQUIRED, minLength = 2) @NotBlank(
        message = "API key name can't be blank") @Size(min = 2,
        message = "API key name must be at least 2 characters") String name,

                                    @Schema(requiredMode = REQUIRED) @NotNull(
                                            message = "Key type is required") KeyType keyType) {
}
