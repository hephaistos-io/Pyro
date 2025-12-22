package io.hephaistos.flagforge.customerapi.exception;

/**
 * Exception thrown when an API key is invalid or not found.
 */
public class InvalidApiKeyException extends RuntimeException {

    public InvalidApiKeyException(String message) {
        super(message);
    }
}
