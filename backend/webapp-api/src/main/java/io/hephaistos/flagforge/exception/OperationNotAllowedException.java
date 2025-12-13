package io.hephaistos.flagforge.exception;

/**
 * Exception thrown when an operation is not allowed due to business rules. For example, attempting
 * to delete a FREE tier environment.
 */
public class OperationNotAllowedException extends RuntimeException {

    public OperationNotAllowedException(String message) {
        super(message);
    }

    public OperationNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }
}
