package io.hephaistos.pyro.exception;

public class CompanyAlreadyAssignedException extends RuntimeException {

    public CompanyAlreadyAssignedException(String message) {
        super(message);
    }

    public CompanyAlreadyAssignedException(String message, Throwable cause) {
        super(message, cause);
    }
}
