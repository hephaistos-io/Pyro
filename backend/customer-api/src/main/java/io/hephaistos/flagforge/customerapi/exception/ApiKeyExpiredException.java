package io.hephaistos.flagforge.customerapi.exception;

/**
 * Exception thrown when an API key has expired.
 */
public class ApiKeyExpiredException extends RuntimeException {

    public ApiKeyExpiredException(String message) {
        super(message);
    }
}
