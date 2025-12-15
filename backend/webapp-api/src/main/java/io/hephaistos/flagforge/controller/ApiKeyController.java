package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.controller.dto.ApiKeyResponse;
import io.hephaistos.flagforge.data.KeyType;
import io.hephaistos.flagforge.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/v1/applications/{applicationId}/environments/{environmentId}/api-keys")
@Tag(name = "api-keys")
@Tag(name = "v1")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Operation(summary = "Get an API key by type for an application and environment")
    @GetMapping(value = "/{keyType}", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ApiKeyResponse getApiKeyByType(@PathVariable UUID applicationId,
            @PathVariable UUID environmentId, @PathVariable KeyType keyType) {
        return apiKeyService.getApiKeyByType(applicationId, environmentId, keyType);
    }

    @Operation(summary = "Regenerate an API key by type and return the new key")
    @PostMapping(value = "/{keyType}/regenerate", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ApiKeyResponse regenerateApiKey(@PathVariable UUID applicationId,
            @PathVariable UUID environmentId, @PathVariable KeyType keyType) {
        return apiKeyService.regenerateKey(applicationId, environmentId, keyType);
    }
}
