package io.hephaistos.flagforge.customerapi.service;

import java.util.Optional;

public interface ApiKeyService {

    Optional<ApiKeyInfo> validateAndGetApplication(String apiKey);
}
