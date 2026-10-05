package com.gireesh.brokerage.controller;

import com.gireesh.brokerage.config.JwtSettings;
import com.gireesh.brokerage.config.SecurityConfiguration;
import com.gireesh.brokerage.dto.AuthUserResponse;
import com.gireesh.brokerage.dto.ChangePasswordRequest;
import com.gireesh.brokerage.dto.LoginRequest;
import com.gireesh.brokerage.dto.RegisterRequest;
import com.gireesh.brokerage.service.AuthService;
import com.gireesh.brokerage.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final JwtSettings jwtSettings;

    public AuthController(
            AuthService authService,
            CurrentUserService currentUserService,
            JwtSettings jwtSettings) {
        this.authService = authService;
        this.currentUserService = currentUserService;
        this.jwtSettings = jwtSettings;
    }

    @PostMapping("/register")
    public AuthUserResponse register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        AuthService.AuthSession session = authService.register(request);
        setAuthCookie(response, session.token(), jwtSettings.getExpiry());
        return session.user();
    }

    @PostMapping("/login")
    public AuthUserResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response) {
        AuthService.AuthSession session = authService.login(request, loginClientKey(request, servletRequest));
        setAuthCookie(response, session.token(), jwtSettings.getExpiry());
        return session.user();
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        setAuthCookie(response, "", Duration.ZERO);
    }

    @GetMapping("/me")
    public AuthUserResponse me() {
        return authService.currentUser(currentUserService.currentUserId());
    }

    @PostMapping("/change-password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(currentUserService.currentUserId(), request);
    }

    private void setAuthCookie(HttpServletResponse response, String token, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(SecurityConfiguration.AUTH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(jwtSettings.isCookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String loginClientKey(LoginRequest request, HttpServletRequest servletRequest) {
        String address = servletRequest.getRemoteAddr();
        return request.email().trim().toLowerCase(Locale.ROOT) + "|" + address;
    }
}
