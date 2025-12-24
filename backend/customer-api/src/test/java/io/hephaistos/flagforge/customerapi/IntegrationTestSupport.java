package io.hephaistos.flagforge.customerapi;

import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Base class for integration tests providing common HTTP request utilities.
 * <p>
 * Usage:
 * <pre>
 * {@code
 * @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
 * @ActiveProfiles("test")
 * @Import(PostgresTestContainerConfiguration.class)
 * @Tag("integration")
 * class MyIntegrationTest extends IntegrationTestSupport {
 *
 *     @BeforeEach
 *     void setUp() {
 *         initializeTestSupport();
 *     }
 * }
 * }
 * </pre>
 */
public abstract class IntegrationTestSupport {

    private static final String API_KEY_HEADER = "X-API-Key";

    @LocalServerPort
    protected int port;

    protected TestRestTemplate restTemplate;

    /**
     * Initialize the test support. Call this in your @BeforeEach method.
     */
    protected void initializeTestSupport() {
        restTemplate = new TestRestTemplate();
    }

    /**
     * Get the base URL for API requests.
     */
    protected String getBaseUrl() {
        return "http://localhost:" + port + "/customer-api";
    }

    // ========== HTTP Request Helpers ==========

    /**
     * Perform a GET request with API key authentication.
     */
    protected <T> ResponseEntity<T> getWithApiKey(String path, String apiKey,
            Class<T> responseType) {
        return restTemplate.exchange(getBaseUrl() + path, HttpMethod.GET,
                new HttpEntity<>(apiKeyHeaders(apiKey)), responseType);
    }

    /**
     * Perform an unauthenticated GET request.
     */
    protected <T> ResponseEntity<T> get(String path, Class<T> responseType) {
        return restTemplate.getForEntity(getBaseUrl() + path, responseType);
    }

    /**
     * Perform a POST request with API key authentication.
     */
    protected <T> ResponseEntity<T> postWithApiKey(String path, String apiKey, Object body,
            Class<T> responseType) {
        var headers = apiKeyHeaders(apiKey);
        headers.set("Content-Type", "application/json");
        return restTemplate.exchange(getBaseUrl() + path, HttpMethod.POST,
                new HttpEntity<>(body, headers), responseType);
    }

    /**
     * Create HTTP headers with API key authentication.
     */
    protected HttpHeaders apiKeyHeaders(String apiKey) {
        var headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, apiKey);
        return headers;
    }
}
