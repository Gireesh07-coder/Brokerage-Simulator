package com.gireesh.brokerage;

import com.gireesh.brokerage.config.SecurityConfiguration;
import com.gireesh.brokerage.entity.Role;
import com.gireesh.brokerage.entity.TradingAccount;
import com.gireesh.brokerage.entity.User;
import com.gireesh.brokerage.repository.TradingAccountRepository;
import com.gireesh.brokerage.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTests {
    private static final String LOCAL_JWT_SECRET =
            "test-only-jwt-secret-not-for-deployment-use-32chars";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TradingAccountRepository tradingAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @BeforeEach
    void cleanAuthData() {
        tradingAccountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void registerCreatesHashedUserAccountAndLogsIn() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"Trader@Example.test","password":"paper1234","displayName":"Trader"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("trader@example.test"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(cookie().httpOnly(SecurityConfiguration.AUTH_COOKIE_NAME, true))
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("SameSite=Lax"),
                                org.hamcrest.Matchers.containsString("Path=/"),
                                org.hamcrest.Matchers.containsString("Max-Age=86400"))))
                .andReturn();

        User user = userRepository.findByEmailIgnoreCase("trader@example.test").orElseThrow();
        assertThat(user.getPasswordHash()).isNotEqualTo("paper1234");
        assertThat(passwordEncoder.matches("paper1234", user.getPasswordHash())).isTrue();
        TradingAccount account = tradingAccountRepository.findByUser(user).orElseThrow();
        assertThat(account.getCash()).isEqualByComparingTo("100000.00");

        Cookie cookie = result.getResponse().getCookie(SecurityConfiguration.AUTH_COOKIE_NAME);
        mockMvc.perform(get("/api/auth/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value("trader@example.test"));
    }

    @Test
    void duplicateEmailIgnoringCaseReturnsFieldError() throws Exception {
        register("Case@Example.test", "Trader One");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"CASE@example.test","password":"paper1234","displayName":"Trader Two"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.email").value("Email is already registered"));
    }

    @Test
    void invalidEmailReturnsFieldError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"paper1234","displayName":"Trader"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").isNotEmpty());
    }

    @Test
    void weakPasswordReturnsFieldError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"weak@example.test","password":"allletters","displayName":"Trader"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").isNotEmpty());
    }

    @Test
    void loginSetsCookieAndMeReturnsCaller() throws Exception {
        register("login@example.test", "Login User");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddress("192.0.2.10"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"LOGIN@example.test","password":"paper1234"}
                                """))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly(SecurityConfiguration.AUTH_COOKIE_NAME, true))
                .andExpect(jsonPath("$.email").value("login@example.test"))
                .andReturn();

        mockMvc.perform(get("/api/auth/me")
                        .cookie(result.getResponse().getCookie(SecurityConfiguration.AUTH_COOKIE_NAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@example.test"));
    }

    @Test
    void unknownEmailAndWrongPasswordHaveIdenticalResponses() throws Exception {
        register("known@example.test", "Known User");

        MvcResult wrongPassword = mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddress("192.0.2.11"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"known@example.test","password":"wrong1234"}
                                """))
                .andExpect(status().isUnauthorized())
                .andReturn();
        MvcResult unknownEmail = mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddress("192.0.2.12"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"unknown@example.test","password":"wrong1234"}
                                """))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(wrongPassword.getResponse().getContentAsString())
                .isNotEqualTo(unknownEmail.getResponse().getContentAsString());
        String wrongMessage = com.jayway.jsonpath.JsonPath.read(
                wrongPassword.getResponse().getContentAsString(), "$.message");
        String unknownMessage = com.jayway.jsonpath.JsonPath.read(
                unknownEmail.getResponse().getContentAsString(), "$.message");
        String wrongError = com.jayway.jsonpath.JsonPath.read(
                wrongPassword.getResponse().getContentAsString(), "$.error");
        String unknownError = com.jayway.jsonpath.JsonPath.read(
                unknownEmail.getResponse().getContentAsString(), "$.error");
        assertThat(wrongMessage).isEqualTo(unknownMessage);
        assertThat(wrongError).isEqualTo(unknownError);
    }

    @Test
    void protectedAccountRequiresAuthenticationAndTamperedOrExpiredJwtIsRejected() throws Exception {
        mockMvc.perform(get("/api/account"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/account")
                        .cookie(new Cookie(SecurityConfiguration.AUTH_COOKIE_NAME, "not.a.valid.token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

        User user = createUser("expired@example.test", "Expired User", Role.USER);
        String expiredToken = token(user, Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600));
        mockMvc.perform(get("/api/account")
                        .cookie(new Cookie(SecurityConfiguration.AUTH_COOKIE_NAME, expiredToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void logoutExpiresCookieAndClientWithoutCookieIsUnauthenticated() throws Exception {
        Cookie authenticated = registrationCookie("logout@example.test");
        mockMvc.perform(post("/api/auth/logout").cookie(authenticated))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("Max-Age=0"),
                                org.hamcrest.Matchers.containsString("Path=/"),
                                org.hamcrest.Matchers.containsString("SameSite=Lax"))));

        mockMvc.perform(get("/api/account"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/account").cookie(authenticated))
                .andExpect(status().isOk());
    }

    @Test
    void accountEndpointOnlyReturnsTheAuthenticatedUsersAccount() throws Exception {
        Cookie userACookie = registrationCookie("user-a@example.test");
        Cookie userBCookie = registrationCookie("user-b@example.test");

        mockMvc.perform(get("/api/account").cookie(userACookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cash").value(100000.00))
                .andExpect(jsonPath("$.id").value(accountIdFor("user-a@example.test")));
        mockMvc.perform(get("/api/account").cookie(userBCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cash").value(100000.00))
                .andExpect(jsonPath("$.id").value(accountIdFor("user-b@example.test")));
    }

    @Test
    void adminEndpointDeniesUserAndReturnsNoPasswordFieldsToAdmin() throws Exception {
        Cookie userCookie = registrationCookie("regular@example.test");
        User admin = createUser("admin@example.test", "Admin", Role.ADMIN);
        Cookie adminCookie = loginCookie("admin@example.test", "paper1234", "192.0.2.20");

        mockMvc.perform(get("/api/admin/users").cookie(userCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/admin/users").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].role").value("ADMIN"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[1].passwordHash").doesNotExist());
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void sixthFailedLoginIsRateLimitedWithRetryAfter() throws Exception {
        for (int attempt = 1; attempt <= 5; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(remoteAddress("192.0.2.30"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"throttle@example.test","password":"wrong1234"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddress("192.0.2.30"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"throttle@example.test","password":"wrong1234"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").value("LOGIN_RATE_LIMITED"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Retry after")));
    }

    @Test
    void successfulLoginResetsFailedAttemptCounter() throws Exception {
        register("reset-limit@example.test", "Reset Limit");
        for (int attempt = 1; attempt <= 4; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(remoteAddress("192.0.2.31"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"reset-limit@example.test","password":"wrong1234"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddress("192.0.2.31"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"reset-limit@example.test","password":"paper1234"}
                                """))
                .andExpect(status().isOk());
        for (int attempt = 1; attempt <= 5; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(remoteAddress("192.0.2.31"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"reset-limit@example.test","password":"wrong1234"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddress("192.0.2.31"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"reset-limit@example.test","password":"wrong1234"}
                                """))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void passwordChangeRequiresCurrentPasswordAndNewPasswordReplacesOld() throws Exception {
        Cookie cookie = registrationCookie("password-change@example.test");
        mockMvc.perform(post("/api/auth/change-password")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"wrong1234","newPassword":"newpaper5678"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_CURRENT_PASSWORD"));

        mockMvc.perform(post("/api/auth/change-password")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"paper1234","newPassword":"newpaper5678"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddress("192.0.2.40"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"password-change@example.test","password":"paper1234"}
                                """))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddress("192.0.2.41"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"password-change@example.test","password":"newpaper5678"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void jwtSettingsRejectsMissingOrShortSecretsOutsideLocalProfile() {
        MockEnvironment production = new MockEnvironment().withProperty("spring.profiles.active", "prod");
        production.setActiveProfiles("prod");
        assertThrows(IllegalStateException.class,
                () -> new com.gireesh.brokerage.config.JwtSettings("", 1440, false, production));
        assertThrows(IllegalStateException.class,
                () -> new com.gireesh.brokerage.config.JwtSettings("short", 1440, false, production));

        com.gireesh.brokerage.config.JwtSettings valid =
                new com.gireesh.brokerage.config.JwtSettings(LOCAL_JWT_SECRET, 1440, false, production);
        assertThat(valid.getExpiryMinutes()).isEqualTo(1440);
    }

    private User createUser(String email, String displayName, Role role) {
        User user = new User(email, passwordEncoder.encode("paper1234"), displayName);
        user.setRole(role);
        User saved = userRepository.saveAndFlush(user);
        tradingAccountRepository.saveAndFlush(
                new TradingAccount(saved, new BigDecimal("100000.00")));
        return saved;
    }

    private void register(String email, String displayName) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"paper1234","displayName":"%s"}
                                """.formatted(email, displayName)))
                .andExpect(status().isOk());
    }

    private Cookie registrationCookie(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"paper1234","displayName":"Test User"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie(SecurityConfiguration.AUTH_COOKIE_NAME);
    }

    private Cookie loginCookie(String email, String password, String address) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddress(address))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie(SecurityConfiguration.AUTH_COOKIE_NAME);
    }

    private Long accountIdFor(String email) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        return tradingAccountRepository.findByUser(user).orElseThrow().getId();
    }

    private String token(User user, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("role", user.getRole().name())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims)).getTokenValue();
    }

    private RequestPostProcessor remoteAddress(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }
}
