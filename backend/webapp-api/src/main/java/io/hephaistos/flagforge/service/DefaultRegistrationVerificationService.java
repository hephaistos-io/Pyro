package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.CustomerEntity;
import io.hephaistos.flagforge.common.data.RegistrationVerificationTokenEntity;
import io.hephaistos.flagforge.configuration.EmailConfiguration;
import io.hephaistos.flagforge.data.repository.CustomerRepository;
import io.hephaistos.flagforge.data.repository.RegistrationVerificationTokenRepository;
import io.hephaistos.flagforge.exception.InvalidTokenException;
import io.hephaistos.flagforge.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
public class DefaultRegistrationVerificationService implements RegistrationVerificationService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DefaultRegistrationVerificationService.class);
    private static final int TOKEN_BYTES = 32;
    private static final Duration TOKEN_EXPIRATION = Duration.ofHours(24);

    private final RegistrationVerificationTokenRepository tokenRepository;
    private final CustomerRepository customerRepository;
    private final EmailService emailService;
    private final EmailConfiguration emailConfiguration;
    private final SecureRandom secureRandom = new SecureRandom();

    public DefaultRegistrationVerificationService(
            RegistrationVerificationTokenRepository tokenRepository,
            CustomerRepository customerRepository, EmailService emailService,
            EmailConfiguration emailConfiguration) {
        this.tokenRepository = tokenRepository;
        this.customerRepository = customerRepository;
        this.emailService = emailService;
        this.emailConfiguration = emailConfiguration;
    }

    @Override
    public void sendVerificationEmail(UUID customerId, String email) {
        // Delete any existing token for this customer
        tokenRepository.findByCustomerId(customerId).ifPresent(tokenRepository::delete);

        // Generate new token
        String token = generateToken();
        Instant now = Instant.now();

        var tokenEntity = new RegistrationVerificationTokenEntity();
        tokenEntity.setToken(token);
        tokenEntity.setCustomerId(customerId);
        tokenEntity.setCreatedAt(now);
        tokenEntity.setExpiresAt(now.plus(TOKEN_EXPIRATION));

        tokenRepository.save(tokenEntity);

        // Build verification URL and send email
        String verificationUrl = buildVerificationUrl(token);
        emailService.sendRegistrationVerificationEmail(email, verificationUrl);

        LOGGER.info("Sent registration verification email to: {}", email);
    }

    @Override
    public void verifyToken(String token) {
        var tokenEntity = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        if (tokenEntity.isUsed()) {
            throw new InvalidTokenException("Verification token has already been used");
        }

        if (tokenEntity.isExpired()) {
            throw new InvalidTokenException("Verification token has expired");
        }

        // Mark token as used
        tokenEntity.setUsedAt(Instant.now());

        // Mark customer as email verified
        var customer = customerRepository.findById(tokenEntity.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        customer.setEmailVerified(true);

        LOGGER.info("Email verified for customer: {}", customer.getEmail());
    }

    @Override
    public void resendVerificationEmail(String email) {
        CustomerEntity customer = customerRepository.findByEmail(email)
                .orElseThrow(
                        () -> new NotFoundException("No account found with this email address"));

        if (customer.isEmailVerified()) {
            throw new IllegalStateException("Email is already verified");
        }

        sendVerificationEmail(customer.getId(), email);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String buildVerificationUrl(String token) {
        return emailConfiguration.getBaseUrl() + "/verify-registration?token=" + token;
    }
}
