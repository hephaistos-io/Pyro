package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.data.PasswordResetTokenEntity;

import java.time.Instant;

public record PasswordResetResponse(String resetUrl, Instant expiresAt) {
    public static PasswordResetResponse fromEntity(PasswordResetTokenEntity entity,
            String baseUrl) {
        return new PasswordResetResponse(baseUrl + "/reset-password?token=" + entity.getToken(),
                entity.getExpiresAt());
    }
}
