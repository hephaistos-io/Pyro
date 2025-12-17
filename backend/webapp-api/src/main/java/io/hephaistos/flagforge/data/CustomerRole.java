package io.hephaistos.flagforge.data;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents the role of a customer within the system. Roles are hierarchical: ADMIN > DEV >
 * READ_ONLY
 *
 * Each higher role includes all permissions of the roles below it: - ADMIN: Full access to all
 * features - DEV: Read/write access to certain elements - READ_ONLY: Only read access
 */
@Schema(enumAsRef = true)
public enum CustomerRole {
    READ_ONLY,
    DEV,
    ADMIN;

    /**
     * Returns the Spring Security authority string for this role. Spring Security expects the ROLE_
     * prefix for hasRole() checks.
     */
    public String toAuthority() {
        return "ROLE_" + this.name();
    }
}
