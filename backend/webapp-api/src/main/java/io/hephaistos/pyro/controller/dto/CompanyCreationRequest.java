package io.hephaistos.pyro.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record CompanyCreationRequest(@Schema(requiredMode = REQUIRED, minLength = 2) @NotBlank(
        message = "Company name can't be blank") String companyName) {

}
