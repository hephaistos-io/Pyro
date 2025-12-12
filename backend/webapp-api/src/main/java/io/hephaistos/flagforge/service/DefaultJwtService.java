package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.configuration.JwtConfiguration;
import io.hephaistos.flagforge.controller.dto.CustomerAuthenticationRequest;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class DefaultJwtService implements JwtService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultJwtService.class);

    private final JwtConfiguration jwtConfiguration;
    private final SecretKey secretKey;

    public DefaultJwtService(JwtConfiguration jwtConfiguration) {
        this.jwtConfiguration = jwtConfiguration;
        this.secretKey = Keys.hmacShaKeyFor(jwtConfiguration.getSecret().getBytes());
    }

    @Override
    public String generateToken(CustomerAuthenticationRequest customerAuthenticationRequest) {
        final var generationTime = Instant.now();

        return Jwts.builder().subject(customerAuthenticationRequest.email())
                .issuedAt(Date.from(generationTime))
                .expiration(Date.from(generationTime.plusSeconds(
                        jwtConfiguration.getExpirationDurationSeconds())))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Deconstructs the token into a clear identifier, that can further be used to create a security
     * context. Also validates the token.
     */
    @Override
    public String decomposeToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            LOGGER.debug("Validating token: {}", token);
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        }
        catch (JwtException e) {
            LOGGER.debug("Invalid token!: {}", e.getMessage());
            return false;
        }
    }

}
