package io.hephaistos.flagforge.exception;

public class NoCompanyAssignedException extends RuntimeException {

    public NoCompanyAssignedException(String message) {
        super(message);
    }

    public NoCompanyAssignedException(String message, Throwable cause) {
        super(message, cause);
    }
}
