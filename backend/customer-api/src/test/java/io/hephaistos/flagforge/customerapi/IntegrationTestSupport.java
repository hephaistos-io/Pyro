package io.hephaistos.flagforge.customerapi;

import io.hephaistos.flagforge.customerapi.data.repository.ApiKeyRepository;
import io.hephaistos.flagforge.customerapi.data.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Base class for integration tests providing common HTTP request utilities and API key helpers.
 */
public abstract class IntegrationTestSupport {

    protected static final String TEST_API_KEY = "ff_test_12345678901234567890123456789012";
    protected static final String TEST_COMPANY_NAME = "Test Company";
    protected static final String TEST_APP_NAME = "Test Application";

    @LocalServerPort
    protected int port;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected ApiKeyRepository apiKeyRepository;

    @Autowired
    protected ApplicationRepository applicationRepository;

    protected TestRestTemplate restTemplate;
    protected UUID testCompanyId;
    protected UUID testApplicationId;
    protected UUID testApiKeyId;

    /**
     * Initialize the test support. Call this in your @BeforeEach method.
     */
    protected void initializeTestSupport() {
        restTemplate = new TestRestTemplate();
        cleanupDatabase();
        setupTestData();
    }

    /**
     * Get the base URL for API requests.
     */
    protected String getBaseUrl() {
        return "http://localhost:" + port + "/customer-api";
    }

    // ========== HTTP Request Helpers ==========

    /**
     * Perform an authenticated GET request with API key.
     */
    protected <T> ResponseEntity<T> get(String path, String apiKey, Class<T> responseType) {
        return restTemplate.exchange(getBaseUrl() + path, HttpMethod.GET,
                new HttpEntity<>(apiKeyHeaders(apiKey)), responseType);
    }

    /**
     * Perform an authenticated GET request with default test API key.
     */
    protected <T> ResponseEntity<T> get(String path, Class<T> responseType) {
        return get(path, TEST_API_KEY, responseType);
    }

    /**
     * Perform an unauthenticated GET request.
     */
    protected <T> ResponseEntity<T> getUnauthenticated(String path, Class<T> responseType) {
        return restTemplate.getForEntity(getBaseUrl() + path, responseType);
    }

    /**
     * Perform an authenticated POST request with API key.
     */
    protected <T> ResponseEntity<T> post(String path, Object body, String apiKey,
            Class<T> responseType) {
        HttpHeaders headers = apiKeyHeaders(apiKey);
        headers.set("Content-Type", "application/json");
        return restTemplate.exchange(getBaseUrl() + path, HttpMethod.POST,
                new HttpEntity<>(body, headers), responseType);
    }

    /**
     * Perform an authenticated POST request with default test API key.
     */
    protected <T> ResponseEntity<T> post(String path, Object body, Class<T> responseType) {
        return post(path, body, TEST_API_KEY, responseType);
    }

    /**
     * Create HTTP headers with X-API-Key authentication.
     */
    protected HttpHeaders apiKeyHeaders(String apiKey) {
        var headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        return headers;
    }

    // ========== Test Data Setup ==========

    private void cleanupDatabase() {
        jdbcTemplate.execute("DELETE FROM api_key");
        jdbcTemplate.execute("DELETE FROM application");
        jdbcTemplate.execute("DELETE FROM company");
    }

    private void setupTestData() {
        // Create test company
        testCompanyId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO company (id, name) VALUES (?, ?)", testCompanyId,
                TEST_COMPANY_NAME);

        // Create test application
        testApplicationId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO application (id, name, company_id) VALUES (?, ?, ?)",
                testApplicationId, TEST_APP_NAME, testCompanyId);

        // Create test API key
        testApiKeyId = UUID.randomUUID();
        String keyHash = hashApiKey(TEST_API_KEY);
        jdbcTemplate.update(
                "INSERT INTO api_key (id, key_hash, key_prefix, application_id, name, is_active, rate_limit_requests_per_minute) " + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                testApiKeyId, keyHash, TEST_API_KEY.substring(0, 8), testApplicationId,
                "Test API Key", true, 1000);
    }

    /**
     * Create an additional API key for testing.
     */
    protected UUID createApiKey(String apiKey, UUID applicationId, boolean isActive,
            int rateLimit) {
        UUID id = UUID.randomUUID();
        String keyHash = hashApiKey(apiKey);
        jdbcTemplate.update(
                "INSERT INTO api_key (id, key_hash, key_prefix, application_id, name, is_active, rate_limit_requests_per_minute) " + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, keyHash, apiKey.substring(0, Math.min(8, apiKey.length())), applicationId,
                "Additional API Key", isActive, rateLimit);
        return id;
    }

    /**
     * Create an expired API key for testing.
     */
    protected UUID createExpiredApiKey(String apiKey, UUID applicationId) {
        UUID id = UUID.randomUUID();
        String keyHash = hashApiKey(apiKey);
        jdbcTemplate.update(
                "INSERT INTO api_key (id, key_hash, key_prefix, application_id, name, is_active, expires_at, rate_limit_requests_per_minute) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id, keyHash, apiKey.substring(0, Math.min(8, apiKey.length())), applicationId,
                "Expired API Key", true, OffsetDateTime.now().minusDays(1), 1000);
        return id;
    }

    private String hashApiKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
