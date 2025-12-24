package io.hephaistos.flagforge.customerapi.controller;

import io.hephaistos.flagforge.customerapi.controller.dto.MergedTemplateValuesResponse;
import io.hephaistos.flagforge.customerapi.security.ApiKeySecurityContext;
import io.hephaistos.flagforge.customerapi.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/v1/api/templates")
@Tag(name = "templates", description = "Template values API for SDK clients")
@SecurityRequirement(name = "apiKey")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @Operation(summary = "Get merged SYSTEM template values",
            description = "Returns merged template values starting with schema defaults, " + "then applying the identifier override if provided. " + "Application and environment are determined from the API key.")
    @GetMapping(value = "/system", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public MergedTemplateValuesResponse getSystemTemplateValues(
            @Parameter(description = "Identifier to apply override for")
            @RequestParam(required = false) String identifier) {

        var securityContext = ApiKeySecurityContext.getCurrent();

        return templateService.getMergedSystemValues(securityContext.getApplicationId(),
                securityContext.getEnvironmentId(), identifier);
    }

    @Operation(summary = "Get merged USER template values for a specific user",
            description = "Returns merged template values applying 3-layer merge: " + "schema defaults → environment defaults → user-specific overrides. " + "Application and environment are determined from the API key.")
    @GetMapping(value = "/user/{userId}", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public MergedTemplateValuesResponse getUserTemplateValues(
            @Parameter(description = "User identifier") @PathVariable String userId) {

        var securityContext = ApiKeySecurityContext.getCurrent();

        return templateService.getMergedUserValues(securityContext.getApplicationId(),
                securityContext.getEnvironmentId(), userId);
    }

    @Operation(summary = "Set USER template overrides for a specific user",
            description = "Creates or updates user-specific override values. " + "Requires a WRITE API key. " + "Application and environment are determined from the API key.")
    @PostMapping(value = "/user/{userId}", consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('WRITE')")
    public void setUserTemplateValues(
            @Parameter(description = "User identifier") @PathVariable String userId,
            @RequestBody Map<String, Object> values) {

        var securityContext = ApiKeySecurityContext.getCurrent();

        templateService.setUserValues(securityContext.getApplicationId(),
                securityContext.getEnvironmentId(), userId, values);
    }
}
