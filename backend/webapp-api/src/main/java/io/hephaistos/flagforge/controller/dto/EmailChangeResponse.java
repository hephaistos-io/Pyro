package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.data.EmailChangeTokenEntity;

import java.time.Instant;

public record EmailChangeResponse(String verificationUrl, String newEmail, Instant expiresAt) {
    public static EmailChangeResponse fromEntity(EmailChangeTokenEntity entity, String baseUrl) {
        return new EmailChangeResponse(baseUrl + "/verify-email?token=" + entity.getToken(),
                entity.getNewEmail(), entity.getExpiresAt());
    }
}
