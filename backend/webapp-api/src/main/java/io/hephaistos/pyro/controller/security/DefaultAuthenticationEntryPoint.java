package io.hephaistos.pyro.controller.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.time.OffsetDateTime;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * This deals with requests that contain the wrong authorization data
 */
public class DefaultAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String WRONG_CREDENTIALS_MESSAGE = """
            {"message": "There was an issue processing your request!", "timestamp": "%s",
            "reason": "%s"}
            """;

    @Override
    @NullMarked
    public void commence(@NotNull HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(APPLICATION_JSON_VALUE);
        response.getWriter()
                .write(WRONG_CREDENTIALS_MESSAGE.formatted(OffsetDateTime.now().format(ISO_INSTANT),
                        authException.getMessage()));
    }
}
