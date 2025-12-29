package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.PasswordResetTokenEntity;
import io.hephaistos.flagforge.controller.dto.PasswordResetResponse;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.data.repository.PasswordResetTokenRepository;
import io.hephaistos.flagforge.exception.BreachedPasswordException;
import io.hephaistos.flagforge.exception.InvalidTokenException;
import io.hephaistos.flagforge.exception.NotFoundException;
import io.hephaistos.flagforge.exception.RateLimitExceededException;
import io.hephaistos.flagforge.security.FlagForgeSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;

@Service
@Transactional
public class DefaultPasswordResetService implements PasswordResetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPasswordResetService.class);
    private static final int TOKEN_LENGTH = 32; // 64 hex chars
    private static final int TOKEN_EXPIRATION_HOURS = 1;
    private static final int MAX_REQUESTS_PER_HOUR = 3;

    private final PasswordResetTokenRepository tokenRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final BreachedPasswordService breachedPasswordService;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public DefaultPasswordResetService(PasswordResetTokenRepository tokenRepository,
            CustomerRepository customerRepository, PasswordEncoder passwordEncoder,
            BreachedPasswordService breachedPasswordService, EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.breachedPasswordService = breachedPasswordService;
        this.emailService = emailService;
    }

    @Override
    public Optional<PasswordResetResponse> requestPasswordReset(String email, String baseUrl) {
        var customerOpt = customerRepository.findByEmail(email.toLowerCase().trim());

        if (customerOpt.isEmpty()) {
            // Return empty to prevent enumeration - caller should still show success message
            LOGGER.info("Password reset requested for non-existent email");
            return Optional.empty();
        }

        var customer = customerOpt.get();

        // Rate limiting check
        var oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long recentRequests = tokenRepository.countRecentRequests(customer.getId(), oneHourAgo);
        if (recentRequests >= MAX_REQUESTS_PER_HOUR) {
            LOGGER.warn("Rate limit exceeded for password reset: {}", email);
            throw new RateLimitExceededException(
                    "Too many password reset requests. Please try again later.");
        }

        // Generate token
        var token = new PasswordResetTokenEntity();
        token.setToken(generateSecureToken());
        token.setCustomerId(customer.getId());
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plus(TOKEN_EXPIRATION_HOURS, ChronoUnit.HOURS));

        tokenRepository.save(token);
        LOGGER.info("Created password reset token for customer {}", customer.getId());

        var response = PasswordResetResponse.fromEntity(token, baseUrl);

        // Send email (mock implementation logs the URL)
        emailService.sendPasswordResetEmail(email, response.resetUrl());

        return Optional.of(response);
    }

    @Override
    public PasswordResetResponse requestPasswordResetAuthenticated(String baseUrl) {
        var securityContext = FlagForgeSecurityContext.getCurrent();
        String email = securityContext.getCustomerName();

        var customer = customerRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        // Rate limiting check
        var oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long recentRequests = tokenRepository.countRecentRequests(customer.getId(), oneHourAgo);
        if (recentRequests >= MAX_REQUESTS_PER_HOUR) {
            LOGGER.warn("Rate limit exceeded for password reset: {}", email);
            throw new RateLimitExceededException(
                    "Too many password reset requests. Please try again later.");
        }

        // Generate token
        var token = new PasswordResetTokenEntity();
        token.setToken(generateSecureToken());
        token.setCustomerId(customer.getId());
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plus(TOKEN_EXPIRATION_HOURS, ChronoUnit.HOURS));

        tokenRepository.save(token);
        LOGGER.info("Created password reset token for authenticated customer {}", customer.getId());

        var response = PasswordResetResponse.fromEntity(token, baseUrl);

        // Send email (mock implementation logs the URL)
        emailService.sendPasswordResetEmail(email, response.resetUrl());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PasswordResetTokenEntity validateToken(String token) {
        var tokenEntity = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset link"));

        if (tokenEntity.isUsed()) {
            throw new InvalidTokenException("This reset link has already been used");
        }

        if (tokenEntity.isExpired()) {
            throw new InvalidTokenException("This reset link has expired");
        }

        return tokenEntity;
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        var tokenEntity = validateToken(token);

        // Check password against HIBP
        if (breachedPasswordService.isPasswordBreached(newPassword)) {
            throw new BreachedPasswordException(
                    "This password has been found in data breaches. Please choose a different password.");
        }

        // Get customer and update password
        var customer = customerRepository.findById(tokenEntity.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        customer.setPassword(passwordEncoder.encode(newPassword));
        customer.setPasswordChangedAt(Instant.now());

        // Mark token as used
        tokenEntity.setUsedAt(Instant.now());

        LOGGER.info("Password reset completed for customer {}", customer.getId());
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }
}
