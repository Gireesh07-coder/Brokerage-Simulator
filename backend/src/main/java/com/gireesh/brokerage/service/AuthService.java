package com.gireesh.brokerage.service;

import com.gireesh.brokerage.config.JwtSettings;
import com.gireesh.brokerage.dto.AuthUserResponse;
import com.gireesh.brokerage.dto.ChangePasswordRequest;
import com.gireesh.brokerage.dto.LoginRequest;
import com.gireesh.brokerage.dto.RegisterRequest;
import com.gireesh.brokerage.entity.Role;
import com.gireesh.brokerage.entity.TradingAccount;
import com.gireesh.brokerage.entity.User;
import com.gireesh.brokerage.exception.ApiException;
import com.gireesh.brokerage.exception.ApiValidationException;
import com.gireesh.brokerage.exception.LoginRateLimitException;
import com.gireesh.brokerage.repository.TradingAccountRepository;
import com.gireesh.brokerage.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {
    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final TradingAccountRepository tradingAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final BigDecimal startingCash;
    private final String dummyPasswordHash;

    public AuthService(
            UserRepository userRepository,
            TradingAccountRepository tradingAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LoginAttemptLimiter loginAttemptLimiter,
            @Value("${app.account.starting-cash:100000.00}") BigDecimal startingCash) {
        this.userRepository = userRepository;
        this.tradingAccountRepository = tradingAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttemptLimiter = loginAttemptLimiter;
        this.startingCash = startingCash;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
        if (startingCash.signum() < 0) {
            throw new IllegalStateException("STARTING_CASH must not be negative");
        }
    }

    @Transactional
    public AuthSession register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiValidationException(
                    "VALIDATION_FAILED",
                    "Request validation failed",
                    Map.of("email", "Email is already registered"));
        }

        User user = new User(email, passwordEncoder.encode(request.password()), request.displayName().trim());
        user.setRole(Role.USER);
        User savedUser = userRepository.saveAndFlush(user);
        TradingAccount account = new TradingAccount(savedUser, startingCash);
        tradingAccountRepository.save(account);
        return new AuthSession(AuthUserResponse.from(savedUser), jwtService.issue(savedUser));
    }

    @Transactional(readOnly = true)
    public AuthSession login(LoginRequest request, String clientKey) {
        long retryAfter = loginAttemptLimiter.retryAfterSeconds(clientKey);
        if (retryAfter > 0) {
            throw new LoginRateLimitException(retryAfter);
        }

        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        String hash = user == null ? dummyPasswordHash : user.getPasswordHash();
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user == null || !matches) {
            loginAttemptLimiter.recordFailure(clientKey);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", INVALID_CREDENTIALS);
        }

        loginAttemptLimiter.recordSuccess(clientKey);
        return new AuthSession(AuthUserResponse.from(user), jwtService.issue(user));
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AuthUserResponse currentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required"));
        return AuthUserResponse.from(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record AuthSession(AuthUserResponse user, String token) {
    }
}
