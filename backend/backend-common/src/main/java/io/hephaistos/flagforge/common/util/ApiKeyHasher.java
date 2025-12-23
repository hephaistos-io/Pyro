package io.hephaistos.flagforge.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility class for hashing API keys using SHA-256.
 * <p>
 * API keys are hashed before storage to prevent exposure in case of database breach. SHA-256 is
 * used because:
 * <ul>
 *   <li>API keys have high entropy (64 hex chars = 256 bits), making brute force infeasible</li>
 *   <li>Deterministic hashing allows direct database lookups</li>
 *   <li>Fast hashing is acceptable for high-entropy secrets (unlike passwords)</li>
 * </ul>
 */
public final class ApiKeyHasher {

    private static final String ALGORITHM = "SHA-256";

    private ApiKeyHasher() {
        // Utility class
    }

    /**
     * Hashes an API key using SHA-256.
     *
     * @param apiKey the plaintext API key
     * @return the SHA-256 hash as a lowercase hex string (64 characters)
     */
    public static String hash(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        }
        catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available in all Java implementations
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
