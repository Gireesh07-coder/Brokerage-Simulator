package com.gireesh.brokerage;

import com.gireesh.brokerage.controller.AccountController;
import com.gireesh.brokerage.controller.TradingController;
import com.gireesh.brokerage.entity.Account;
import com.gireesh.brokerage.entity.Portfolio;
import com.gireesh.brokerage.entity.Stock;
import com.gireesh.brokerage.entity.TransactionType;
import com.gireesh.brokerage.exception.ApiError;
import com.gireesh.brokerage.exception.GlobalExceptionHandler;
import com.gireesh.brokerage.exception.TradingException;
import com.gireesh.brokerage.repository.AccountRepository;
import com.gireesh.brokerage.repository.PortfolioRepository;
import com.gireesh.brokerage.repository.StockRepository;
import com.gireesh.brokerage.repository.TransactionRepository;
import com.gireesh.brokerage.service.AccountService;
import com.gireesh.brokerage.service.StockService;
import com.gireesh.brokerage.service.TradingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PhaseTwoBehaviorTests {

    @Mock
    private AccountService accountService;

    @Mock
    private TradingService tradingService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private StockService stockService;

    @Test
    void missingAccountReturnsStructuredNotFoundResponse() throws Exception {
        when(accountService.getAccount(999L))
                .thenThrow(new com.gireesh.brokerage.exception.AccountNotFoundException(999L));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AccountController(accountService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/api/accounts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Account not found with id: 999"));
    }

    @Test
    void zeroQuantityReturnsBadRequest() throws Exception {
        when(tradingService.buyStock(1L, 1L, 0))
                .thenThrow(new TradingException(
                        "INVALID_QUANTITY", "Quantity must be greater than zero"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TradingController(tradingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/trading/buy")
                        .param("accountId", "1")
                        .param("stockId", "1")
                        .param("quantity", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_QUANTITY"));
    }

    @Test
    void negativeQuantityReturnsBadRequest() throws Exception {
        when(tradingService.buyStock(1L, 1L, -1))
                .thenThrow(new TradingException(
                        "INVALID_QUANTITY", "Quantity must be greater than zero"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TradingController(tradingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/trading/buy")
                        .param("accountId", "1")
                        .param("stockId", "1")
                        .param("quantity", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_QUANTITY"));
    }

    @Test
    void insufficientFundsDoesNotChangeCashOrPosition() {
        Account account = new Account("Test", new BigDecimal("10.00"));
        Stock stock = new Stock("TEST", "Test Security", new BigDecimal("100.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(stockRepository.findById(1L)).thenReturn(Optional.of(stock));

        TradingService service = new TradingService(
                accountRepository, stockRepository, portfolioRepository, transactionRepository);

        TradingException exception = assertThrows(
                TradingException.class,
                () -> service.buyStock(1L, 1L, 1));

        assertThat(exception.getErrorCode()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(account.getCashBalance()).isEqualByComparingTo("10.00");
        verify(accountRepository, never()).save(any(Account.class));
        verify(portfolioRepository, never()).save(any(Portfolio.class));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void sellingFullPositionRecordsTransactionAndDeletesPosition() {
        Account account = new Account("Test", new BigDecimal("100.00"));
        Stock stock = new Stock("TEST", "Test Security", new BigDecimal("10.00"));
        Portfolio portfolio = new Portfolio(account, stock, 5);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(stockRepository.findById(1L)).thenReturn(Optional.of(stock));
        when(portfolioRepository.findByAccountAndStock(account, stock))
                .thenReturn(Optional.of(portfolio));

        TradingService service = new TradingService(
                accountRepository, stockRepository, portfolioRepository, transactionRepository);

        service.sellStock(1L, 1L, 5);

        assertThat(account.getCashBalance()).isEqualByComparingTo("149.95");
        assertThat(portfolio.getQuantity()).isZero();
        verify(portfolioRepository).deleteByAccountAndStock(account, stock);
        verify(portfolioRepository, never()).save(portfolio);
        ArgumentCaptor<com.gireesh.brokerage.entity.Transaction> transactionCaptor =
                ArgumentCaptor.forClass(com.gireesh.brokerage.entity.Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getType()).isEqualTo(TransactionType.SELL);
        assertThat(transactionCaptor.getValue().getQuantity()).isEqualTo(5);
    }

    @Test
    void duplicateStockSymbolIsRejectedBeforePersistence() {
        when(stockRepository.existsBySymbolIgnoreCase("TEST")).thenReturn(true);

        TradingException exception = assertThrows(
                TradingException.class,
                () -> stockService.createStock(
                        " test ", "Test Security", new BigDecimal("10.00")));

        assertThat(exception.getErrorCode()).isEqualTo("DUPLICATE_SYMBOL");
        verify(stockRepository, never()).save(any(Stock.class));
    }
}
