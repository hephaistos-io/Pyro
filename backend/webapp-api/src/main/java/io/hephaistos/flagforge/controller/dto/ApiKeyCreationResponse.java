package io.hephaistos.flagforge.controller.dto;

import java.util.UUID;

public record ApiKeyCreationResponse(UUID id, String name, String secretKey) {
}
