package io.hephaistos.flagforge.common.data;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import io.hephaistos.flagforge.common.security.FlagForgeSecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for entities that need audit tracking. Provides automatic population of created/updated
 * timestamps and user references via JPA lifecycle callbacks.
 * <p>
 * Entities extending this class will have their audit fields automatically set:
 * <ul>
 *   <li>On create: createdAt, updatedAt, createdBy, updatedBy are all set</li>
 *   <li>On update: updatedAt and updatedBy are updated</li>
 * </ul>
 * <p>
 * When no security context is available (e.g., system operations, batch jobs), the SYSTEM_USER_ID
 * is used as the user reference.
 */
@MappedSuperclass
public abstract class AuditableEntity {

    /**
     * UUID used to identify system-initiated operations when no user context is available.
     * This value should be displayed as "System" in the UI.
     */
    public static final UUID SYSTEM_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        UUID userId = getCurrentUserId();
        createdBy = userId;
        updatedBy = userId;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        updatedBy = getCurrentUserId();
    }

    /**
     * Attempts to get the current user ID from the security context. Returns SYSTEM_USER_ID if no
     * security context is available or if the context is not a FlagForgeSecurityContext.
     * <p>
     * This handles different security contexts (e.g., ApiKeySecurityContext in customer-api)
     * by defaulting to SYSTEM_USER_ID when the context doesn't have a customer ID.
     */
    private UUID getCurrentUserId() {
        try {
            var context = SecurityContextHolder.getContext();
            if (context instanceof FlagForgeSecurityContext flagForgeContext) {
                UUID customerId = flagForgeContext.getCustomerId();
                return customerId != null ? customerId : SYSTEM_USER_ID;
            }
            return SYSTEM_USER_ID;
        } catch (Exception e) {
            return SYSTEM_USER_ID;
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }
}
