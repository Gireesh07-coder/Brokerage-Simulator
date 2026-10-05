package com.gireesh.brokerage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class JwtSettings {
    private final String secret;
    private final long expiryMinutes;
    private final boolean cookieSecure;
    private final Environment environment;

    public JwtSettings(
            @Value("${app.jwt.secret:}") String secret,
            @Value("${app.jwt.expiry-minutes:1440}") long expiryMinutes,
            @Value("${app.cookie.secure:false}") boolean cookieSecure,
            Environment environment) {
        this.secret = secret;
        this.expiryMinutes = expiryMinutes;
        this.cookieSecure = cookieSecure;
        this.environment = environment;

        if (expiryMinutes < 1) {
            throw new IllegalStateException("JWT_EXPIRY_MINUTES must be greater than zero");
        }
        if (secret.length() < 32
                && !environment.acceptsProfiles(Profiles.of("local"))) {
            throw new IllegalStateException(
                    "JWT_SECRET must contain at least 32 characters outside the local profile");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException("The local JWT_SECRET must contain at least 32 characters");
        }
    }

    public String getSecret() {
        return secret;
    }

    public Duration getExpiry() {
        return Duration.ofMinutes(expiryMinutes);
    }

    public long getExpiryMinutes() {
        return expiryMinutes;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public boolean isLocalProfile() {
        return environment.acceptsProfiles(Profiles.of("local"));
    }
}
