package com.gireesh.brokerage;

import com.gireesh.brokerage.config.SecurityConfiguration;
import com.gireesh.brokerage.entity.Security;
import com.gireesh.brokerage.repository.SecurityRepository;
import com.gireesh.brokerage.repository.TransactionRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TradingAtomicityTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityRepository securityRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private TransactionRepository transactionRepository;

    @Test
    void failureWritingLedgerRollsBackOrderCashAndPosition() throws Exception {
        String email = "atomic-" + UUID.randomUUID() + "@example.test";
        MvcResult registered = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"paper1234","displayName":"Atomicity"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = registered.getResponse().getCookie(SecurityConfiguration.AUTH_COOKIE_NAME);
        Number userId = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/api/auth/me").cookie(cookie))
                        .andReturn().getResponse().getContentAsString(),
                "$.id");
        Long accountId = jdbcTemplate.queryForObject(
                "select id from trading_accounts where user_id = ?", Long.class, userId.longValue());
        Security security = securityRepository.findBySymbolIgnoreCase("TCS").orElseThrow();

        doThrow(new DataAccessResourceFailureException("simulated ledger persistence failure"))
                .when(transactionRepository).saveAndFlush(any());

        mockMvc.perform(post("/api/orders")
                        .cookie(cookie)
                        .header("Idempotency-Key", "atomicity-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"securityId":%d,"side":"BUY","quantity":1}
                                """.formatted(security.getId())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"));

        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "select cash from trading_accounts where id = ?", java.math.BigDecimal.class, accountId))
                .isEqualByComparingTo("100000.00");
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "select count(*) from orders where account_id = ?", Long.class, accountId))
                .isZero();
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "select count(*) from positions where account_id = ?", Long.class, accountId))
                .isZero();
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "select count(*) from transactions where trading_account_id = ?", Long.class, accountId))
                .isZero();
    }
}
