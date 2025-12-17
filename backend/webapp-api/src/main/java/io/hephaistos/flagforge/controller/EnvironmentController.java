package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.controller.dto.EnvironmentCreationRequest;
import io.hephaistos.flagforge.controller.dto.EnvironmentResponse;
import io.hephaistos.flagforge.service.EnvironmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/v1/applications/{applicationId}/environments")
@Tag(name = "environments")
public class EnvironmentController {

    private final EnvironmentService environmentService;

    public EnvironmentController(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @Operation(summary = "Create a new environment for an application")
    @PostMapping(value = "", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public EnvironmentResponse createEnvironment(@PathVariable UUID applicationId,
            @Valid @RequestBody EnvironmentCreationRequest request) {
        return environmentService.createEnvironment(applicationId, request);
    }

    @Operation(summary = "List all environments for an application")
    @GetMapping(value = "", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public List<EnvironmentResponse> getEnvironments(@PathVariable UUID applicationId) {
        return environmentService.getEnvironmentsForApplication(applicationId);
    }

    @Operation(summary = "Delete an environment. FREE tier environments cannot be deleted.")
    @DeleteMapping(value = "/{environmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEnvironment(@PathVariable UUID applicationId,
            @PathVariable UUID environmentId) {
        environmentService.deleteEnvironment(applicationId, environmentId);
    }
}
