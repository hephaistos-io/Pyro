package io.hephaistos.flagforge.customerapi.exception;

/**
 * Exception thrown when a requested resource cannot be found.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
