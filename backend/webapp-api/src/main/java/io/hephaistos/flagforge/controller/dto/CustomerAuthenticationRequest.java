package io.hephaistos.flagforge.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record CustomerAuthenticationRequest(
        @Schema(example = "your@email.com", requiredMode = REQUIRED) @NotBlank(
                message = "E-Mail can't be blank") @Email(
                message = "E-Mail has to be valid!") String email,
        @Schema(requiredMode = REQUIRED) @NotBlank(
                message = "password can't be blank") String password) {

    public UsernamePasswordAuthenticationToken toUsernamePasswordAuthenticationToken() {
        return new UsernamePasswordAuthenticationToken(email, password);
    }
}