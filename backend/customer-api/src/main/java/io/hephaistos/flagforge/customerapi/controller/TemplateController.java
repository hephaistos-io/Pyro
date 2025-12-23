package io.hephaistos.flagforge.customerapi.controller;

import io.hephaistos.flagforge.customerapi.controller.dto.MergedTemplateValuesResponse;
import io.hephaistos.flagforge.customerapi.security.ApiKeySecurityContext;
import io.hephaistos.flagforge.customerapi.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
}
