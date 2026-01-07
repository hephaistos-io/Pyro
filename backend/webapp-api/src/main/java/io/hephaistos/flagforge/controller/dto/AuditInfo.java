package io.hephaistos.flagforge.controller.dto;

import java.time.Instant;

/**
 * Audit information DTO for exposing who created/modified an entity and when.
 * User IDs are resolved to display names server-side for privacy.
 */
public record AuditInfo(
        Instant createdAt,
        Instant updatedAt,
        String createdByName,
        String updatedByName
) {
}
