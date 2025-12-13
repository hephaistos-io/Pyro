package io.hephaistos.flagforge.customerapi.service;

import java.util.UUID;

public record ApiKeyInfo(UUID apiKeyId, UUID applicationId, UUID companyId,
                         int rateLimitPerMinute) {
}
