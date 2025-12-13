package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.controller.dto.ApiKeyCreationRequest;
import io.hephaistos.flagforge.controller.dto.ApiKeyCreationResponse;
import io.hephaistos.flagforge.controller.dto.ApiKeyResponse;
import io.hephaistos.flagforge.service.ApiKeyService;
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
@RequestMapping("/v1/applications/{applicationId}/api-keys")
@Tag(name = "api-keys")
@Tag(name = "v1")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Operation(summary = "Create a new API key for an application")
    @PostMapping(value = "", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiKeyCreationResponse createApiKey(@PathVariable UUID applicationId,
            @Valid @RequestBody ApiKeyCreationRequest request) {
        return apiKeyService.createApiKey(applicationId, request.name());
    }

    @Operation(summary = "List all API keys for an application")
    @GetMapping(value = "", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public List<ApiKeyResponse> getApiKeys(@PathVariable UUID applicationId) {
        return apiKeyService.getApiKeysForApplication(applicationId);
    }

    @Operation(summary = "Revoke an API key")
    @DeleteMapping(value = "/{apiKeyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeApiKey(@PathVariable UUID applicationId, @PathVariable UUID apiKeyId) {
        apiKeyService.revokeApiKey(applicationId, apiKeyId);
    }
}
