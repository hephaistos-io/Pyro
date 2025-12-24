package io.hephaistos.flagforge.common.util;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Utility for converting user identifier strings to deterministic UUIDs. This allows customers to
 * use any identifier format (email, numeric ID, UUID string, etc.) while we store a native UUID for
 * optimal database performance.
 */
public final class UserIdHasher {

    private static final String PREFIX = "flagforge:user:";

    private UserIdHasher() {
    }

    /**
     * Converts a user identifier string to a deterministic UUID. Same input always produces the
     * same UUID.
     *
     * <p>
     * Uses UUID v3 (MD5-based name UUID). This is safe for this use case because:
     * <ul>
     * <li>Collision probability depends on output size (128 bits), not hash algorithm</li>
     * <li>For 1 billion users: collision probability ≈ 10^-20</li>
     * <li>MD5's weakness is intentional collision crafting, not accidental collisions</li>
     * <li>User IDs are not chosen by attackers to exploit hash weaknesses</li>
     * </ul>
     *
     * @param userId the user identifier string from the customer
     * @return a deterministic UUID derived from the identifier
     */
    public static UUID toUuid(String userId) {
        return UUID.nameUUIDFromBytes((PREFIX + userId).getBytes(StandardCharsets.UTF_8));
    }
}
