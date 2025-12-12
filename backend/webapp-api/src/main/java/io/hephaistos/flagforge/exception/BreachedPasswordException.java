package io.hephaistos.flagforge.exception;

public class BreachedPasswordException extends RuntimeException {

    public BreachedPasswordException(String message) {
        super(message);
    }

    public BreachedPasswordException(String message, Throwable cause) {
        super(message, cause);
    }
}
