package io.hephaistos.flagforge.exception;

public class InvalidInviteException extends RuntimeException {

    private final InvalidInviteReason reason;

    public InvalidInviteException(String message, InvalidInviteReason reason) {
        super(message);
        this.reason = reason;
    }

    public InvalidInviteReason getReason() {
        return reason;
    }

    public enum InvalidInviteReason {
        NOT_FOUND,
        EXPIRED,
        ALREADY_USED
    }
}
