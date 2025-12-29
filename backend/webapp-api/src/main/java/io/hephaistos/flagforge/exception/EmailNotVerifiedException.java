package io.hephaistos.flagforge.exception;

/**
 * Exception thrown when a user attempts to log in without having verified their email address.
 */
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
