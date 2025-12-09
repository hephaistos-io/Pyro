package io.hephaistos.pyro.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class BreachedPasswordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BreachedPasswordService.class);
    private static final String HIBP_API_URL = "https://api.pwnedpasswords.com/range/";
    private final RestClient restClient;

    public BreachedPasswordService() {
        this.restClient = RestClient.create();
    }

    /**
     * Checks if password appears in known data breaches using k-anonymity model. Only sends first 5
     * chars of SHA-1 hash to API (privacy-preserving).
     *
     * @param password The password to check
     * @return true if password found in breaches, false otherwise
     */
    public boolean isPasswordBreached(String password) {
        try {
            String sha1Hash = sha1Hex(password).toUpperCase();
            String prefix = sha1Hash.substring(0, 5);
            String suffix = sha1Hash.substring(5);

            String response =
                    restClient.get().uri(HIBP_API_URL + prefix).retrieve().body(String.class);

            boolean breached = response != null && response.contains(suffix);

            if (breached) {
                LOGGER.warn("Password found in breach database (hash prefix: {})", prefix);
            }

            return breached;

        }
        catch (Exception e) {
            // Fail open - don't block registration if API is down
            LOGGER.error("Failed to check password against HIBP API - allowing registration", e);
            return false;
        }
    }

    /**
     * Generates SHA-1 hash of input string and returns hex representation. Uses Java's built-in
     * MessageDigest - no external dependencies needed.
     *
     * @param input The string to hash
     * @return Hex representation of SHA-1 hash
     * @throws NoSuchAlgorithmException if SHA-1 algorithm is not available
     */
    private String sha1Hex(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = String.format("%02x", b);
            hexString.append(hex);
        }

        return hexString.toString();
    }
}
