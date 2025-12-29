package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.EmailChangeTokenEntity;
import io.hephaistos.flagforge.controller.dto.EmailChangeResponse;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.data.repository.EmailChangeTokenRepository;
import io.hephaistos.flagforge.exception.DuplicateResourceException;
import io.hephaistos.flagforge.exception.InvalidTokenException;
import io.hephaistos.flagforge.exception.NotFoundException;
import io.hephaistos.flagforge.security.FlagForgeSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
public class DefaultEmailChangeService implements EmailChangeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEmailChangeService.class);
    private static final int TOKEN_LENGTH = 32; // 64 hex chars
    private static final int TOKEN_EXPIRATION_HOURS = 1;

    private final EmailChangeTokenRepository tokenRepository;
    private final CustomerRepository customerRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public DefaultEmailChangeService(EmailChangeTokenRepository tokenRepository,
            CustomerRepository customerRepository, EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.customerRepository = customerRepository;
        this.emailService = emailService;
    }

    @Override
    public EmailChangeResponse requestEmailChange(String newEmail, String baseUrl) {
        var securityContext = FlagForgeSecurityContext.getCurrent();
        UUID customerId = securityContext.getCustomerId();

        String normalizedEmail = newEmail.toLowerCase().trim();

        // Check if email is already taken
        if (customerRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateResourceException("This email is already registered");
        }

        // Invalidate any pending email change requests
        tokenRepository.invalidatePendingRequests(customerId, Instant.now());

        // Create new token
        var token = new EmailChangeTokenEntity();
        token.setToken(generateSecureToken());
        token.setCustomerId(customerId);
        token.setNewEmail(normalizedEmail);
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plus(TOKEN_EXPIRATION_HOURS, ChronoUnit.HOURS));

        tokenRepository.save(token);
        LOGGER.info("Created email change token for customer {} to {}", customerId,
                normalizedEmail);

        var response = EmailChangeResponse.fromEntity(token, baseUrl);

        // Send verification email to the NEW email address
        emailService.sendEmailChangeVerification(normalizedEmail, response.verificationUrl());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public EmailChangeTokenEntity validateToken(String token) {
        var tokenEntity = tokenRepository.findByToken(token)
                .orElseThrow(
                        () -> new InvalidTokenException("Invalid or expired verification link"));

        if (tokenEntity.isUsed()) {
            throw new InvalidTokenException("This verification link has already been used");
        }

        if (tokenEntity.isExpired()) {
            throw new InvalidTokenException("This verification link has expired");
        }

        return tokenEntity;
    }

    @Override
    public void confirmEmailChange(String token) {
        var tokenEntity = validateToken(token);

        // Check if new email is still available
        if (customerRepository.findByEmail(tokenEntity.getNewEmail()).isPresent()) {
            throw new DuplicateResourceException("This email is no longer available");
        }

        var customer = customerRepository.findById(tokenEntity.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        String oldEmail = customer.getEmail();
        customer.setEmail(tokenEntity.getNewEmail());

        tokenEntity.setUsedAt(Instant.now());

        LOGGER.info("Email changed for customer {} from {} to {}", customer.getId(), oldEmail,
                tokenEntity.getNewEmail());
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }
}
