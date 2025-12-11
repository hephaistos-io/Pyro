package io.hephaistos.pyro;

import io.hephaistos.pyro.controller.dto.AuthenticationResponse;
import io.hephaistos.pyro.controller.dto.CompanyCreationRequest;
import io.hephaistos.pyro.controller.dto.UserAuthenticationRequest;
import io.hephaistos.pyro.controller.dto.UserRegistrationRequest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Base class for integration tests providing common HTTP request utilities and authentication
 * helpers.
 * <p>
 * Extends {@link MockPasswordCheck} to include password breach service mocking.
 * <p>
 * Usage:
 * <pre>
 * {@code
 * @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
 * @Tag("integration")
 * class MyIntegrationTest extends IntegrationTestSupport {
 *
 *     @BeforeEach
 *     void setUp() {
 *         initializeTestSupport();
 *         // your cleanup code
 *     }
 * }
 * }
 * </pre>
 */
public abstract class IntegrationTestSupport extends MockPasswordCheck {

    private static final String DEFAULT_EMAIL = "test@example.com";
    private static final String DEFAULT_PASSWORD = "password123";
    private static final String DEFAULT_FIRST_NAME = "John";
    private static final String DEFAULT_LAST_NAME = "Doe";
    private static final String DEFAULT_COMPANY_NAME = "Test Company";

    @LocalServerPort
    protected int port;

    protected TestRestTemplate restTemplate;

    /**
     * Initialize the test support. Call this in your @BeforeEach method.
     */
    protected void initializeTestSupport() {
        restTemplate = new TestRestTemplate();
        mockPasswordBreachCheckWithResponse(false);
    }

    /**
     * Get the base URL for API requests.
     */
    protected String getBaseUrl() {
        return "http://localhost:" + port + "/api";
    }

    // ========== HTTP Request Helpers ==========

    /**
     * Perform an authenticated GET request.
     */
    protected <T> ResponseEntity<T> get(String path, String token, Class<T> responseType) {
        return restTemplate.exchange(getBaseUrl() + path, HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), responseType);
    }

    /**
     * Perform an unauthenticated GET request.
     */
    protected <T> ResponseEntity<T> get(String path, Class<T> responseType) {
        return restTemplate.getForEntity(getBaseUrl() + path, responseType);
    }

    /**
     * Perform an authenticated POST request.
     */
    protected <T> ResponseEntity<T> post(String path, Object body, String token,
            Class<T> responseType) {
        return restTemplate.exchange(getBaseUrl() + path, HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(token)), responseType);
    }

    /**
     * Perform an unauthenticated POST request.
     */
    protected <T> ResponseEntity<T> post(String path, Object body, Class<T> responseType) {
        return restTemplate.postForEntity(getBaseUrl() + path, body, responseType);
    }

    /**
     * Perform an authenticated PUT request.
     */
    protected <T> ResponseEntity<T> put(String path, Object body, String token,
            Class<T> responseType) {
        return restTemplate.exchange(getBaseUrl() + path, HttpMethod.PUT,
                new HttpEntity<>(body, authHeaders(token)), responseType);
    }

    /**
     * Perform an authenticated DELETE request.
     */
    protected <T> ResponseEntity<T> delete(String path, String token, Class<T> responseType) {
        return restTemplate.exchange(getBaseUrl() + path, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)), responseType);
    }

    /**
     * Create HTTP headers with Bearer token authentication.
     */
    protected HttpHeaders authHeaders(String token) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    // ========== Authentication Helpers ==========

    /**
     * Register a user with default credentials and authenticate. Returns the authentication token.
     */
    protected String registerAndAuthenticate() {
        registerUser();
        return authenticate();
    }

    /**
     * Register a user with default credentials, create a company, and authenticate. Returns the
     * authentication token (with companyId populated in security context).
     */
    protected String registerAndAuthenticateWithCompany() {
        registerUser();
        String token = authenticate();
        createCompany(token);
        // Re-authenticate to get token with companyId populated in security context
        return authenticate();
    }

    /**
     * Register a user with custom credentials.
     */
    protected void registerUser(String firstName, String lastName, String email, String password) {
        var registration = new UserRegistrationRequest(firstName, lastName, email, password);
        restTemplate.postForEntity(getBaseUrl() + "/v1/auth/register", registration, Void.class);
    }

    /**
     * Register a user with default credentials.
     */
    protected void registerUser() {
        registerUser(DEFAULT_FIRST_NAME, DEFAULT_LAST_NAME, DEFAULT_EMAIL, DEFAULT_PASSWORD);
    }

    /**
     * Authenticate with custom credentials and return the token.
     */
    protected String authenticate(String email, String password) {
        var authRequest = new UserAuthenticationRequest(email, password);
        var response =
                restTemplate.postForEntity(getBaseUrl() + "/v1/auth/authenticate", authRequest,
                        AuthenticationResponse.class);
        return response.getBody().token();
    }

    /**
     * Authenticate with default credentials and return the token.
     */
    protected String authenticate() {
        return authenticate(DEFAULT_EMAIL, DEFAULT_PASSWORD);
    }

    /**
     * Create a company with the given name for the authenticated user.
     */
    protected void createCompany(String token, String companyName) {
        post("/v1/company", new CompanyCreationRequest(companyName), token, String.class);
    }

    /**
     * Create a company with the default name for the authenticated user.
     */
    protected void createCompany(String token) {
        createCompany(token, DEFAULT_COMPANY_NAME);
    }
}
