package io.hephaistos.flagforge.controller;

import io.hephaistos.flagforge.exception.BreachedPasswordException;
import io.hephaistos.flagforge.exception.CompanyAlreadyAssignedException;
import io.hephaistos.flagforge.exception.DuplicateResourceException;
import io.hephaistos.flagforge.exception.InvalidInviteException;
import io.hephaistos.flagforge.exception.NoCompanyAssignedException;
import io.hephaistos.flagforge.exception.NotFoundException;
import io.hephaistos.flagforge.exception.OperationNotAllowedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        LOGGER.warn("Invalid request: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex) {
        LOGGER.warn("User not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("INVALID_CREDENTIALS", "Invalid email or password"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        LOGGER.warn("Invalid credentials attempt: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("INVALID_CREDENTIALS", "Invalid email or password"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        LOGGER.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("INVALID_CREDENTIALS", "Invalid email or password"));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex) {
        LOGGER.warn("Duplicate resource: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE_RESOURCE", ex.getMessage()));
    }

    @ExceptionHandler(BreachedPasswordException.class)
    public ResponseEntity<ErrorResponse> handleBreachedPassword(BreachedPasswordException ex) {
        LOGGER.warn("Breached password attempt: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("BREACHED_PASSWORD", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        LOGGER.warn("Validation failed: {}", errorMessage);
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", errorMessage));
    }

    @ExceptionHandler(NoCompanyAssignedException.class)
    public ResponseEntity<ErrorResponse> handleNoCompanyAssigned(NoCompanyAssignedException ex) {
        LOGGER.info("Customer has no company assigned! {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("MISSING_COMPANY_ASSIGNMENT",
                        "The customer has no company assigned."));

    }

    @ExceptionHandler(CompanyAlreadyAssignedException.class)
    public ResponseEntity<ErrorResponse> handleCompanyAlreadyAssigned(
            CompanyAlreadyAssignedException ex) {
        LOGGER.info("Customer has a company already assigned! {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("EXISTING_COMPANY_ASSIGNMENT",
                        "The customer has a company assigned."));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex) {
        LOGGER.info("Operation didn't find required resource {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", "Requested resource was not found."));
    }

    @ExceptionHandler(OperationNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleOperationNotAllowed(
            OperationNotAllowedException ex) {
        LOGGER.warn("Operation not allowed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("OPERATION_NOT_ALLOWED", ex.getMessage()));
    }

    @ExceptionHandler(InvalidInviteException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInvite(InvalidInviteException ex) {
        LOGGER.warn("Invalid invite: {} (reason: {})", ex.getMessage(), ex.getReason());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_INVITE", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        LOGGER.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    public record ErrorResponse(String code, String message) {
    }
}
