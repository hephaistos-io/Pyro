package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.common.enums.TemplateType;
import io.hephaistos.flagforge.controller.dto.AllTemplateOverridesResponse;
import io.hephaistos.flagforge.controller.dto.CopyOverridesRequest;
import io.hephaistos.flagforge.controller.dto.CopyOverridesResponse;
import io.hephaistos.flagforge.controller.dto.MergedTemplateValuesResponse;
import io.hephaistos.flagforge.controller.dto.TemplateResponse;
import io.hephaistos.flagforge.controller.dto.TemplateUpdateRequest;
import io.hephaistos.flagforge.controller.dto.TemplateValuesRequest;
import io.hephaistos.flagforge.controller.dto.TemplateValuesResponse;
import io.hephaistos.flagforge.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/v1/applications/{applicationId}/templates")
@Tag(name = "templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    // ========== Template CRUD ==========
    // Note: Templates are created automatically when an application is created.
    // Customers can only list, view, and update templates - not create or delete them.

    @Operation(summary = "List templates for an application, optionally filtered by type")
    @GetMapping(value = "", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public List<TemplateResponse> getTemplates(@PathVariable UUID applicationId, @Parameter(
            description = "Filter by template type (USER or SYSTEM). If not provided, returns all templates.")
    @RequestParam(required = false) TemplateType type) {
        if (type != null) {
            return List.of(templateService.getTemplateByType(applicationId, type));
        }
        return templateService.getTemplates(applicationId);
    }

    @Operation(summary = "Update a template's schema")
    @PutMapping(value = "/{type}", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TemplateResponse updateTemplate(@PathVariable UUID applicationId,
            @PathVariable TemplateType type, @Valid @RequestBody TemplateUpdateRequest request) {
        return templateService.updateTemplate(applicationId, type, request);
    }

    // ========== Template Values (Overrides) ==========

    @Operation(
            summary = "List all overrides for an application and environment, grouped by template type")
    @GetMapping(value = "/overrides", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public AllTemplateOverridesResponse listAllOverrides(@PathVariable UUID applicationId,
            @RequestParam UUID environmentId) {
        return templateService.listAllOverrides(applicationId, environmentId);
    }

    @Operation(summary = "Get merged values for a template (defaults + identifier overrides)")
    @GetMapping(value = "/{type}/environments/{environmentId}/values",
            produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public MergedTemplateValuesResponse getMergedValues(@PathVariable UUID applicationId,
            @PathVariable TemplateType type, @PathVariable UUID environmentId,
            @Parameter(description = "Identifiers to merge, in order of precedence")
            @RequestParam(required = false) List<String> identifiers) {
        return templateService.getMergedValues(applicationId, environmentId, type, identifiers);
    }

    @Operation(summary = "List all overrides for a template in an environment")
    @GetMapping(value = "/{type}/environments/{environmentId}/overrides",
            produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public List<TemplateValuesResponse> listOverrides(@PathVariable UUID applicationId,
            @PathVariable TemplateType type, @PathVariable UUID environmentId) {
        return templateService.listOverrides(applicationId, environmentId, type);
    }

    @Operation(summary = "Set or update override values for a specific identifier")
    @PutMapping(value = "/{type}/environments/{environmentId}/overrides/{identifier}",
            produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TemplateValuesResponse setOverride(@PathVariable UUID applicationId,
            @PathVariable TemplateType type, @PathVariable UUID environmentId,
            @PathVariable String identifier, @Valid @RequestBody TemplateValuesRequest request) {
        return templateService.setOverride(applicationId, environmentId, type, identifier, request);
    }

    @Operation(summary = "Delete an override for a specific identifier")
    @DeleteMapping(value = "/{type}/environments/{environmentId}/overrides/{identifier}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOverride(@PathVariable UUID applicationId, @PathVariable TemplateType type,
            @PathVariable UUID environmentId, @PathVariable String identifier) {
        templateService.deleteOverride(applicationId, environmentId, type, identifier);
    }

    @Operation(summary = "Copy overrides from one environment to another")
    @PostMapping(value = "/copy-overrides", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public CopyOverridesResponse copyOverrides(@PathVariable UUID applicationId,
            @Valid @RequestBody CopyOverridesRequest request) {
        return templateService.copyOverrides(applicationId, request);
    }
}
