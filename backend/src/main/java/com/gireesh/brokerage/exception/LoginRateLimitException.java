package com.gireesh.brokerage.exception;

import org.springframework.http.HttpStatus;

public class LoginRateLimitException extends ApiException {
    private final long retryAfterSeconds;

    public LoginRateLimitException(long retryAfterSeconds) {
        super(
                HttpStatus.TOO_MANY_REQUESTS,
                "LOGIN_RATE_LIMITED",
                "Too many failed login attempts. Retry after " + retryAfterSeconds + " seconds.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
