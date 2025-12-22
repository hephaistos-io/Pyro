package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.data.CompanyInviteEntity;
import io.hephaistos.flagforge.common.enums.CustomerRole;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record InviteCreationResponse(UUID id, String token, String email, String inviteUrl,
                                     CustomerRole role, Set<UUID> applicationIds,
                                     Instant expiresAt) {
    public static InviteCreationResponse fromEntity(CompanyInviteEntity entity, String baseUrl) {
        return new InviteCreationResponse(entity.getId(), entity.getToken(), entity.getEmail(),
                baseUrl + "/register?invite=" + entity.getToken(), entity.getAssignedRole(),
                entity.getPreAssignedApplicationIds(), entity.getExpiresAt());
    }
}
