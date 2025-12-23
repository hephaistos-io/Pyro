package io.hephaistos.flagforge.customerapi.exception;

/**
 * Exception thrown when a rate limit is exceeded.
 */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterMillis;

    public RateLimitExceededException(String message, long retryAfterMillis) {
        super(message);
        this.retryAfterMillis = retryAfterMillis;
    }

    /**
     * Get the number of milliseconds the client should wait before retrying.
     *
     * @return milliseconds to wait
     */
    public long getRetryAfterMillis() {
        return retryAfterMillis;
    }

    /**
     * Get the number of seconds the client should wait before retrying. Rounds up to the nearest
     * second.
     *
     * @return seconds to wait
     */
    public long getRetryAfterSeconds() {
        return (retryAfterMillis + 999) / 1000;
    }
}
