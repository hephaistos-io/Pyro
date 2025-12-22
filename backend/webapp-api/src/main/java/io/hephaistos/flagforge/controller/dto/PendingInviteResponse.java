package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.data.CompanyInviteEntity;
import io.hephaistos.flagforge.common.enums.CustomerRole;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record PendingInviteResponse(UUID id, String email, CustomerRole role,
                                    Set<UUID> applicationIds, Instant expiresAt,
                                    Instant createdAt) {
    public static PendingInviteResponse fromEntity(CompanyInviteEntity entity) {
        return new PendingInviteResponse(entity.getId(), entity.getEmail(),
                entity.getAssignedRole(), entity.getPreAssignedApplicationIds(),
                entity.getExpiresAt(), entity.getCreatedAt());
    }
}
