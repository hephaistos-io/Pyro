package io.hephaistos.flagforge.data;

/**
 * Represents the role of a customer within the system. Roles are hierarchical: ADMIN > DEV >
 * READ_ONLY
 *
 * Each higher role includes all permissions of the roles below it: - ADMIN: Full access to all
 * features - DEV: Read/write access to certain elements - READ_ONLY: Only read access
 */
public enum CustomerRole {
    READ_ONLY,
    DEV,
    ADMIN
}
