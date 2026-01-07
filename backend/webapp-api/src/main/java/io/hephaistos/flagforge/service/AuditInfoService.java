package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.AuditableEntity;
import io.hephaistos.flagforge.controller.dto.AuditInfo;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for resolving audit information based on user role.
 * Audit info is only visible to ADMIN and DEV roles, not READ_ONLY.
 */
public interface AuditInfoService {

    /**
     * Returns true if the current user has permission to view audit information.
     * Only ADMIN and DEV roles can view audit info.
     */
    boolean canViewAuditInfo();

    /**
     * Resolves a user ID to a display name.
     * Returns "System" for SYSTEM_USER_ID, or the user's full name otherwise.
     */
    String resolveUserName(UUID userId);

    /**
     * Creates an AuditInfo DTO from an auditable entity.
     * Returns null if the current user doesn't have permission to view audit info.
     */
    AuditInfo createAuditInfo(AuditableEntity entity);

    /**
     * Returns the createdAt timestamp if the user has permission, null otherwise.
     */
    Instant getCreatedAtIfAllowed(AuditableEntity entity);

    /**
     * Returns the updatedAt timestamp if the user has permission, null otherwise.
     */
    Instant getUpdatedAtIfAllowed(AuditableEntity entity);

    /**
     * Returns the createdByName if the user has permission, null otherwise.
     */
    String getCreatedByNameIfAllowed(AuditableEntity entity);

    /**
     * Returns the updatedByName if the user has permission, null otherwise.
     */
    String getUpdatedByNameIfAllowed(AuditableEntity entity);
}
