package com.gireesh.brokerage;

import com.gireesh.brokerage.config.SecurityConfiguration;
import com.gireesh.brokerage.entity.Order;
import com.gireesh.brokerage.entity.OrderStatus;
import com.gireesh.brokerage.entity.Position;
import com.gireesh.brokerage.entity.Security;
import com.gireesh.brokerage.entity.TradingAccount;
import com.gireesh.brokerage.entity.Transaction;
import com.gireesh.brokerage.entity.TransactionType;
import com.gireesh.brokerage.entity.User;
import com.gireesh.brokerage.repository.OrderRepository;
import com.gireesh.brokerage.repository.PositionRepository;
import com.gireesh.brokerage.repository.SecurityRepository;
import com.gireesh.brokerage.repository.TradingAccountRepository;
import com.gireesh.brokerage.repository.TransactionRepository;
import com.gireesh.brokerage.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TradingIntegrationTests {
    private static final String SYMBOL = "TCS";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TradingAccountRepository tradingAccountRepository;

    @Autowired
    private SecurityRepository securityRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private Security security;
    private Cookie callerCookie;
    private Cookie secondaryCookie;
    private TradingAccount callerAccount;

    @BeforeEach
    void setUp() throws Exception {
        transactionRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        positionRepository.deleteAllInBatch();
        tradingAccountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        security = securityRepository.findBySymbolIgnoreCase(SYMBOL).orElseThrow();
        security.setCurrentPrice(new BigDecimal("10.00"));
        securityRepository.saveAndFlush(security);
        secondaryCookie = null;
        callerCookie = createAccount("trader");
    }

    @Test
    void buyPersistsExecutedOrderTransactionAndUpdatesCashAndPosition() throws Exception {
        MvcResult result = place("buy-success", "BUY", 3, security.getId(), callerCookie)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.side").value("BUY"))
                .andExpect(jsonPath("$.type").value("MARKET"))
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.executedPrice").value(10.00))
                .andReturn();

        assertThat(account().getCash()).isEqualByComparingTo("99970.00");
        Position position = position();
        assertThat(position.getQuantity()).isEqualTo(3);
        assertThat(position.getAverageCost()).isEqualByComparingTo("10.000000");
        Order order = orderRepository.findAll().getFirst();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXECUTED);
        assertThat(transactionRepository.count()).isEqualTo(1);
        Transaction transaction = transactionRepository.findAll().getFirst();
        assertThat(transaction.getType()).isEqualTo(TransactionType.BUY);
        assertThat(transaction.getQuantity()).isEqualTo(3);
        assertThat(transaction.getTradingAccount().getId()).isEqualTo(callerAccount.getId());
        assertThat(transaction.getOrder().getId()).isEqualTo(order.getId());
        assertThat(transaction.getRealizedPnl()).isEqualByComparingTo("0.00");
        assertThat(result.getResponse().getContentAsString()).contains("\"status\":\"EXECUTED\"");
    }

    @Test
    void sellPersistsExecutedOrderAndTransaction() throws Exception {
        createPosition(5, "8.00");

        place("sell-success", "SELL", 2, security.getId(), callerCookie)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        assertThat(account().getCash()).isEqualByComparingTo("100020.00");
        assertThat(position().getQuantity()).isEqualTo(3);
        assertThat(transactionRepository.findAll().getFirst().getType()).isEqualTo(TransactionType.SELL);
    }

    @Test
    void insufficientCashPersistsRejectedOrderWithoutFinancialWrites() throws Exception {
        callerAccount.setCash(new BigDecimal("1.00"));
        tradingAccountRepository.saveAndFlush(callerAccount);

        place("insufficient-cash", "BUY", 1, security.getId(), callerCookie)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_CASH"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.message").value("Insufficient cash to execute this buy order"))
                .andExpect(jsonPath("$.errors").isEmpty());

        assertThat(account().getCash()).isEqualByComparingTo("1.00");
        assertThat(orderRepository.findAll()).singleElement()
                .extracting(Order::getStatus).isEqualTo(OrderStatus.REJECTED);
        assertThat(positionRepository.count()).isZero();
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void insufficientHoldingsPersistsRejectedOrderWithoutFinancialWrites() throws Exception {
        place("insufficient-holdings", "SELL", 1, security.getId(), callerCookie)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_HOLDINGS"));

        assertThat(account().getCash()).isEqualByComparingTo("100000.00");
        assertThat(orderRepository.findAll()).singleElement()
                .extracting(Order::getStatus).isEqualTo(OrderStatus.REJECTED);
        assertThat(positionRepository.count()).isZero();
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void zeroAndNegativeQuantitiesReturnStandardValidationErrorsWithoutConsumingKey() throws Exception {
        for (int quantity : new int[]{0, -1}) {
            place("invalid-quantity", "BUY", quantity, security.getId(), callerCookie)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors.quantity").isNotEmpty());
        }

        assertThat(orderRepository.count()).isZero();
        place("invalid-quantity", "BUY", 1, security.getId(), callerCookie)
                .andExpect(status().isCreated());
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void buysRecalculateWeightedAverageCost() throws Exception {
        place("weighted-buy-one", "BUY", 2, security.getId(), callerCookie)
                .andExpect(status().isCreated());
        security.setCurrentPrice(new BigDecimal("11.00"));
        securityRepository.saveAndFlush(security);

        place("weighted-buy-two", "BUY", 3, security.getId(), callerCookie)
                .andExpect(status().isCreated());

        assertThat(position().getQuantity()).isEqualTo(5);
        assertThat(position().getAverageCost()).isEqualByComparingTo("10.600000");
        assertThat(account().getInvestedCapital()).isEqualByComparingTo("53.00");
    }

    @Test
    void partialSellPreservesPositionAndUpdatesRealizedPnl() throws Exception {
        createPosition(5, "8.00");

        place("partial-sell", "SELL", 2, security.getId(), callerCookie)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        assertThat(position().getQuantity()).isEqualTo(3);
        assertThat(position().getAverageCost()).isEqualByComparingTo("8.000000");
        assertThat(position().getRealizedPnl()).isEqualByComparingTo("4.00");
        assertThat(account().getRealizedPnl()).isEqualByComparingTo("4.00");
        assertThat(account().getInvestedCapital()).isEqualByComparingTo("24.00");
        assertThat(transactionRepository.findAll().getFirst().getRealizedPnl())
                .isEqualByComparingTo("4.00");
    }

    @Test
    void fullSellClosesPositionAndRealizesPnl() throws Exception {
        createPosition(2, "8.00");

        place("full-sell", "SELL", 2, security.getId(), callerCookie)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        assertThat(positionRepository.count()).isZero();
        assertThat(account().getRealizedPnl()).isEqualByComparingTo("4.00");
        assertThat(account().getInvestedCapital()).isEqualByComparingTo("0.00");
        assertThat(account().getCash()).isEqualByComparingTo("100020.00");
    }

    @Test
    void chargesAreZeroAndLedgerAmountsAreDeterministicallyRounded() throws Exception {
        security.setCurrentPrice(new BigDecimal("1.235"));
        securityRepository.saveAndFlush(security);

        place("zero-charge-rounding", "BUY", 3, security.getId(), callerCookie)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.executedPrice").value(1.24))
                .andExpect(jsonPath("$.charges").value(0.00));

        Order order = orderRepository.findAll().getFirst();
        Transaction transaction = transactionRepository.findAll().getFirst();
        assertThat(order.getCharges()).isEqualByComparingTo("0.00");
        assertThat(transaction.getPrice()).isEqualByComparingTo("1.24");
        assertThat(transaction.getTradeValue()).isEqualByComparingTo("3.72");
        assertThat(transaction.getBrokerage()).isEqualByComparingTo("0.00");
        assertThat(transaction.getTotalAmount()).isEqualByComparingTo("3.72");
    }

    @Test
    void callerScopedReadsDoNotExposeAnotherUsersOrdersPositionsOrTrades() throws Exception {
        place("caller-one-buy", "BUY", 2, security.getId(), callerCookie)
                .andExpect(status().isCreated());
        Cookie secondCookie = createAccount("other");
        place("caller-two-buy", "BUY", 4, security.getId(), secondCookie)
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/orders").cookie(callerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].quantity").value(2));
        mockMvc.perform(get("/api/positions").cookie(callerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].quantity").value(2));
        mockMvc.perform(get("/api/transactions").cookie(callerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].quantity").value(2));
        mockMvc.perform(get("/api/orders/" + orderFor(secondCookie).getId()).cookie(callerCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void clientSuppliedAccountIdCannotOverrideAuthenticatedOwnership() throws Exception {
        createAccount("account-override-target");
        Number otherUserId = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/api/auth/me").cookie(secondaryCookie))
                        .andReturn().getResponse().getContentAsString(),
                "$.id");
        TradingAccount otherAccount = tradingAccountRepository.findByUserId(otherUserId.longValue())
                .orElseThrow();

        mockMvc.perform(post("/api/orders")
                        .cookie(callerCookie)
                        .header("Idempotency-Key", "account-override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":%d,"securityId":%d,"side":"BUY","quantity":1}
                                """.formatted(otherAccount.getId(), security.getId())))
                .andExpect(status().isCreated());

        assertThat(account().getCash()).isEqualByComparingTo("99990.00");
        assertThat(tradingAccountRepository.findById(otherAccount.getId()).orElseThrow().getCash())
                .isEqualByComparingTo("100000.00");
    }

    @Test
    void successfulDuplicateSubmissionReplaysOriginalOrderWithoutSecondTrade() throws Exception {
        var first = place("idempotent-success", "BUY", 2, security.getId(), callerCookie)
                .andExpect(status().isCreated())
                .andReturn();
        var retry = place("idempotent-success", "BUY", 2, security.getId(), callerCookie)
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(retry.getResponse().getContentAsString())
                .isEqualTo(first.getResponse().getContentAsString());
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(transactionRepository.count()).isEqualTo(1);
        assertThat(account().getCash()).isEqualByComparingTo("99980.00");
    }

    @Test
    void duplicateRejectedSubmissionReplaysRejectionWithoutFinancialWrites() throws Exception {
        callerAccount.setCash(new BigDecimal("1.00"));
        tradingAccountRepository.saveAndFlush(callerAccount);

        MvcResult first = place("idempotent-rejected", "BUY", 1, security.getId(), callerCookie)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_CASH"))
                .andReturn();
        MvcResult retry = place("idempotent-rejected", "BUY", 1, security.getId(), callerCookie)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_CASH"))
                .andReturn();

        String firstMessage = com.jayway.jsonpath.JsonPath.read(
                first.getResponse().getContentAsString(), "$.message");
        String retryMessage = com.jayway.jsonpath.JsonPath.read(
                retry.getResponse().getContentAsString(), "$.message");
        assertThat(retryMessage).isEqualTo(firstMessage);
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(transactionRepository.count()).isZero();
        assertThat(account().getCash()).isEqualByComparingTo("1.00");
    }

    @Test
    void reusingIdempotencyKeyForDifferentPayloadReturnsConflict() throws Exception {
        place("idempotency-conflict", "BUY", 1, security.getId(), callerCookie)
                .andExpect(status().isCreated());

        place("idempotency-conflict", "BUY", 2, security.getId(), callerCookie)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("IDEMPOTENCY_KEY_REUSED"));

        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void differentPayloadWithUnknownSecurityStillConflictsOnPreviouslyUsedKey() throws Exception {
        place("idempotency-security-conflict", "BUY", 1, security.getId(), callerCookie)
                .andExpect(status().isCreated());

        place("idempotency-security-conflict", "BUY", 1, Long.MAX_VALUE, callerCookie)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    void differentAccountsMayReuseTheSameIdempotencyKey() throws Exception {
        place("shared-idempotency", "BUY", 1, security.getId(), callerCookie)
                .andExpect(status().isCreated());
        Cookie secondCookie = createAccount("shared-key");

        place("shared-idempotency", "BUY", 1, security.getId(), secondCookie)
                .andExpect(status().isCreated());

        assertThat(orderRepository.count()).isEqualTo(2);
        assertThat(transactionRepository.count()).isEqualTo(2);
    }

    @Test
    void unknownSecurityDoesNotConsumeIdempotencyKey() throws Exception {
        place("unknown-security-key", "BUY", 1, Long.MAX_VALUE, callerCookie)
                .andExpect(status().isNotFound());

        assertThat(orderRepository.count()).isZero();
        assertThat(account().getCash()).isEqualByComparingTo("100000.00");
        place("unknown-security-key", "BUY", 1, security.getId(), callerCookie)
                .andExpect(status().isCreated());
    }

    @Test
    void simultaneousBuysCannotOverspendAccountCash() throws Exception {
        callerAccount.setCash(new BigDecimal("10.00"));
        tradingAccountRepository.saveAndFlush(callerAccount);

        int[] statuses = submitConcurrently(
                "concurrent-buy-a", "concurrent-buy-b", "BUY", 1);

        assertThat(statuses)
                .as("cash=%s position=%s", account().getCash(), positionRepository.findAll())
                .containsExactlyInAnyOrder(201, 422);
        assertThat(account().getCash()).isEqualByComparingTo("0.00");
        assertThat(position().getQuantity()).isEqualTo(1);
        assertThat(transactionRepository.count()).isEqualTo(1);
        assertThat(orderRepository.count()).isEqualTo(2);
    }

    @Test
    void simultaneousSellsCannotOversellPosition() throws Exception {
        createPosition(1, "10.00");

        int[] statuses = submitConcurrently(
                "concurrent-sell-a", "concurrent-sell-b", "SELL", 1);

        assertThat(statuses).containsExactlyInAnyOrder(201, 422);
        assertThat(positionRepository.count()).isZero();
        assertThat(transactionRepository.count()).isEqualTo(1);
        assertThat(orderRepository.count()).isEqualTo(2);
    }

    @Test
    void unknownSecurityErrorUsesTheStandardErrorShape() throws Exception {
        place("unknown-security-shape", "BUY", 1, Long.MAX_VALUE, callerCookie)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("SECURITY_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Security not found"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void corsAllowsOnlyConfiguredOriginAndRequiredIdempotencyHeader() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .options("/api/orders")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,idempotency-key"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Headers",
                                org.hamcrest.Matchers.containsString("idempotency-key")));
    }

    private int[] submitConcurrently(String keyOne, String keyTwo, String side, int quantity)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> first = executor.submit(() -> concurrentRequest(
                    ready, start, keyOne, side, quantity));
            Future<Integer> second = executor.submit(() -> concurrentRequest(
                    ready, start, keyTwo, side, quantity));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return new int[]{first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)};
        } finally {
            executor.shutdownNow();
        }
    }

    private int concurrentRequest(
            CountDownLatch ready, CountDownLatch start, String key, String side, int quantity)
            throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent request start timed out");
        }
        return place(key, side, quantity, security.getId(), callerCookie)
                .andReturn().getResponse().getStatus();
    }

    private org.springframework.test.web.servlet.ResultActions place(
            String key, String side, int quantity, Long securityId, Cookie cookie) throws Exception {
        return mockMvc.perform(post("/api/orders")
                .cookie(cookie)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"securityId":%d,"side":"%s","quantity":%d}
                        """.formatted(securityId, side, quantity)));
    }

    private Cookie createAccount(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@example.test";
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"paper1234","displayName":"Trader"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        TradingAccount account = tradingAccountRepository.findByUserId(user.getId()).orElseThrow();
        if (prefix.equals("trader")) {
            callerAccount = account;
        } else {
            secondaryCookie = result.getResponse().getCookie(SecurityConfiguration.AUTH_COOKIE_NAME);
        }
        return result.getResponse().getCookie(SecurityConfiguration.AUTH_COOKIE_NAME);
    }

    private TradingAccount account() {
        return tradingAccountRepository.findById(callerAccount.getId()).orElseThrow();
    }

    private Position position() {
        return positionRepository.findByAccountAndSecurity(account(), security).orElseThrow();
    }

    private void createPosition(int quantity, String averageCost) {
        Position position = new Position();
        position.setAccount(callerAccount);
        position.setSecurity(security);
        position.setQuantity(quantity);
        position.setAverageCost(new BigDecimal(averageCost));
        position.setRealizedPnl(BigDecimal.ZERO);
        positionRepository.saveAndFlush(position);
        callerAccount.setInvestedCapital(
                new BigDecimal(averageCost).multiply(BigDecimal.valueOf(quantity)));
        tradingAccountRepository.saveAndFlush(callerAccount);
    }

    private Order orderFor(Cookie cookie) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .cookie(cookie)
                        .header("Idempotency-Key", "second-user-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"securityId":%d,"side":"BUY","quantity":4}
                                """.formatted(security.getId())))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return orderRepository.findById(id.longValue()).orElseThrow();
    }

}
