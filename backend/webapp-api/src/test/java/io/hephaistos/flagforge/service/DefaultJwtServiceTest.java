package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.configuration.JwtConfiguration;
import io.hephaistos.flagforge.controller.dto.UserAuthenticationRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class DefaultJwtServiceTest {

    private static final String TEST_SECRET =
            "test-secret-key-for-testing-purposes-only-must-be-long-enough";
    private static final int TEST_EXPIRATION_SECONDS = 3600; // 1 hour
    private static final String TEST_EMAIL = "test@example.com";

    private DefaultJwtService jwtService;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        JwtConfiguration config = new JwtConfiguration();
        config.setSecret(TEST_SECRET);
        config.setExpirationDurationSeconds(TEST_EXPIRATION_SECONDS);

        jwtService = new DefaultJwtService(config);
        secretKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
    }

    @Test
    void generatesValidToken() {
        UserAuthenticationRequest request = new UserAuthenticationRequest(TEST_EMAIL, "password");

        String token = jwtService.generateToken(request);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    void generatedTokenContainsCorrectSubject() {
        UserAuthenticationRequest request = new UserAuthenticationRequest(TEST_EMAIL, "password");

        String token = jwtService.generateToken(request);

        String subject = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

        assertThat(subject).isEqualTo(TEST_EMAIL);
    }

    @Test
    void generatedTokenHasCorrectExpiration() {
        UserAuthenticationRequest request = new UserAuthenticationRequest(TEST_EMAIL, "password");
        Instant beforeGeneration = Instant.now();

        String token = jwtService.generateToken(request);

        Instant afterGeneration = Instant.now();

        Date expiration = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();

        // Expiration should be around current time + TEST_EXPIRATION_SECONDS
        // Note: JWT timestamps are in seconds precision, so we need a wider range
        Instant expectedExpiration =
                beforeGeneration.plusSeconds(TEST_EXPIRATION_SECONDS).minusSeconds(2);
        Instant maxExpectedExpiration =
                afterGeneration.plusSeconds(TEST_EXPIRATION_SECONDS).plusSeconds(2);

        assertThat(expiration.toInstant()).isBetween(expectedExpiration, maxExpectedExpiration);
    }

    @Test
    void validTokenPassesValidation() {
        UserAuthenticationRequest request = new UserAuthenticationRequest(TEST_EMAIL, "password");
        String token = jwtService.generateToken(request);

        boolean isValid = jwtService.validateToken(token);

        assertThat(isValid).isTrue();
    }

    @Test
    void expiredTokenFailsValidation() {
        // Create a token with immediate expiration
        JwtConfiguration expiredConfig = new JwtConfiguration();
        expiredConfig.setSecret(TEST_SECRET);
        expiredConfig.setExpirationDurationSeconds(-1); // Already expired

        DefaultJwtService expiredJwtService = new DefaultJwtService(expiredConfig);
        UserAuthenticationRequest request = new UserAuthenticationRequest(TEST_EMAIL, "password");
        String expiredToken = expiredJwtService.generateToken(request);

        boolean isValid = jwtService.validateToken(expiredToken);

        assertThat(isValid).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"not.a.token", "invalid", "header.payload",  // Missing signature
            "a.b.c.d.e"        // Too many parts
    })
    void malformedTokenFailsValidation(String invalidToken) {
        boolean isValid = jwtService.validateToken(invalidToken);

        assertThat(isValid).isFalse();
    }

    @Test
    void nullTokenThrowsException() {
        assertThatThrownBy(() -> jwtService.validateToken(null)).isInstanceOf(
                IllegalArgumentException.class).hasMessageContaining("cannot be null or empty");
    }

    @Test
    void emptyTokenThrowsException() {
        assertThatThrownBy(() -> jwtService.validateToken("")).isInstanceOf(
                IllegalArgumentException.class).hasMessageContaining("cannot be null or empty");
    }

    @Test
    void tamperedTokenFailsValidation() {
        UserAuthenticationRequest request = new UserAuthenticationRequest(TEST_EMAIL, "password");
        String validToken = jwtService.generateToken(request);

        // Tamper with the token by modifying the signature
        String[] parts = validToken.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + ".tampered_signature";

        boolean isValid = jwtService.validateToken(tamperedToken);

        assertThat(isValid).isFalse();
    }

    @Test
    void tokenSignedWithDifferentSecretFailsValidation() {
        // Create a token with a different secret
        JwtConfiguration differentConfig = new JwtConfiguration();
        differentConfig.setSecret(
                "different-secret-key-that-is-long-enough-for-hmac-sha-algorithm");
        differentConfig.setExpirationDurationSeconds(TEST_EXPIRATION_SECONDS);

        DefaultJwtService differentSecretService = new DefaultJwtService(differentConfig);
        UserAuthenticationRequest request = new UserAuthenticationRequest(TEST_EMAIL, "password");
        String tokenWithDifferentSecret = differentSecretService.generateToken(request);

        boolean isValid = jwtService.validateToken(tokenWithDifferentSecret);

        assertThat(isValid).isFalse();
    }

    @Test
    void decomposeTokenExtractsCorrectEmail() {
        UserAuthenticationRequest request = new UserAuthenticationRequest(TEST_EMAIL, "password");
        String token = jwtService.generateToken(request);

        String extractedEmail = jwtService.decomposeToken(token);

        assertThat(extractedEmail).isEqualTo(TEST_EMAIL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"test@example.com", "user.name+tag@example.co.uk",
            "john.doe@subdomain.example.com", "admin@localhost"})
    void decomposeTokenExtractsVariousEmailFormats(String email) {
        UserAuthenticationRequest request = new UserAuthenticationRequest(email, "password");
        String token = jwtService.generateToken(request);

        String extractedEmail = jwtService.decomposeToken(token);

        assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    void decomposeInvalidTokenThrowsException() {
        String invalidToken = "invalid.token.here";

        assertThatThrownBy(() -> jwtService.decomposeToken(invalidToken)).isInstanceOf(
                Exception.class);
    }

    @Test
    void decomposeTamperedTokenThrowsException() {
        UserAuthenticationRequest request = new UserAuthenticationRequest(TEST_EMAIL, "password");
        String validToken = jwtService.generateToken(request);

        // Tamper with the token
        String[] parts = validToken.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + ".tampered";

        assertThatThrownBy(() -> jwtService.decomposeToken(tamperedToken)).isInstanceOf(
                Exception.class);
    }

    @Test
    void decomposeExpiredTokenThrowsException() {
        // Create a token with immediate expiration
        JwtConfiguration expiredConfig = new JwtConfiguration();
        expiredConfig.setSecret(TEST_SECRET);
        expiredConfig.setExpirationDurationSeconds(-1);

        DefaultJwtService expiredJwtService = new DefaultJwtService(expiredConfig);
        UserAuthenticationRequest request = new UserAuthenticationRequest(TEST_EMAIL, "password");
        String expiredToken = expiredJwtService.generateToken(request);

        assertThatThrownBy(() -> jwtService.decomposeToken(expiredToken)).isInstanceOf(
                Exception.class);
    }

    @Test
    void generatedTokenIsImmediatelyValidAndDecomposable() {
        UserAuthenticationRequest request = new UserAuthenticationRequest(TEST_EMAIL, "password");

        String token = jwtService.generateToken(request);

        // Token should be valid
        assertThat(jwtService.validateToken(token)).isTrue();

        // Token should be decomposable
        String extractedEmail = jwtService.decomposeToken(token);
        assertThat(extractedEmail).isEqualTo(TEST_EMAIL);
    }

    @Test
    void multipleTokensForSameUserAreDifferent() throws InterruptedException {
        UserAuthenticationRequest request = new UserAuthenticationRequest(TEST_EMAIL, "password");

        String token1 = jwtService.generateToken(request);

        // Wait to ensure different issuedAt timestamp (JWT timestamps have second precision)
        Thread.sleep(1100);

        String token2 = jwtService.generateToken(request);

        // Tokens should be different due to different issuedAt times
        assertThat(token1).isNotEqualTo(token2);

        // But both should be valid
        assertThat(jwtService.validateToken(token1)).isTrue();
        assertThat(jwtService.validateToken(token2)).isTrue();

        // Both should decompose to the same email
        assertThat(jwtService.decomposeToken(token1)).isEqualTo(TEST_EMAIL);
        assertThat(jwtService.decomposeToken(token2)).isEqualTo(TEST_EMAIL);
    }
}
