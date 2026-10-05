package com.gireesh.brokerage.service;

import com.gireesh.brokerage.entity.Account;
import com.gireesh.brokerage.entity.Portfolio;
import com.gireesh.brokerage.entity.Stock;
import com.gireesh.brokerage.entity.Transaction;
import com.gireesh.brokerage.entity.TransactionType;
import com.gireesh.brokerage.exception.AccountNotFoundException;
import com.gireesh.brokerage.exception.StockNotFoundException;
import com.gireesh.brokerage.exception.TradingException;
import com.gireesh.brokerage.repository.AccountRepository;
import com.gireesh.brokerage.repository.PortfolioRepository;
import com.gireesh.brokerage.repository.StockRepository;
import com.gireesh.brokerage.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class TradingService {

    private final AccountRepository accountRepository;
    private final StockRepository stockRepository;
    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;

    public TradingService(
            AccountRepository accountRepository,
            StockRepository stockRepository,
            PortfolioRepository portfolioRepository,
            TransactionRepository transactionRepository) {

        this.accountRepository = accountRepository;
        this.stockRepository = stockRepository;
        this.portfolioRepository = portfolioRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Portfolio buyStock(
            Long accountId,
            Long stockId,
            Integer quantity) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(accountId));

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() ->
                        new StockNotFoundException(stockId));

        if (quantity == null || quantity <= 0) {
            throw new TradingException(
                    "INVALID_QUANTITY", "Quantity must be greater than zero");
        }

        BigDecimal tradeValue =
                stock.getCurrentPrice()
                        .multiply(BigDecimal.valueOf(quantity));

        BigDecimal brokerage =
                tradeValue.multiply(new BigDecimal("0.001"));

        BigDecimal totalCost =
                tradeValue.add(brokerage);

        if (account.getCashBalance().compareTo(totalCost) < 0) {
            throw new TradingException(
                    "INSUFFICIENT_FUNDS",
                    "Available cash is " + account.getCashBalance()
                            + " but the order requires " + totalCost + ".");
        }

        account.setCashBalance(
                account.getCashBalance().subtract(totalCost));

        accountRepository.save(account);

        Portfolio portfolio =
                portfolioRepository
                        .findByAccountAndStock(account, stock)
                        .orElse(null);

        if (portfolio == null) {

            portfolio = new Portfolio(
                    account,
                    stock,
                    quantity
            );

        } else {

            portfolio.setQuantity(
                    portfolio.getQuantity() + quantity
            );
        }

        Portfolio savedPortfolio =
                portfolioRepository.save(portfolio);

        Transaction transaction = new Transaction(
                account,
                stock,
                TransactionType.BUY,
                quantity,
                stock.getCurrentPrice(),
                tradeValue,
                brokerage,
                totalCost,
                LocalDateTime.now()
        );

        transactionRepository.save(transaction);

        return savedPortfolio;
    }

    @Transactional
    public Portfolio sellStock(
            Long accountId,
            Long stockId,
            Integer quantity) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(accountId));

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() ->
                        new StockNotFoundException(stockId));

        if (quantity == null || quantity <= 0) {
            throw new TradingException(
                    "INVALID_QUANTITY", "Quantity must be greater than zero");
        }

        Portfolio portfolio =
                portfolioRepository
                        .findByAccountAndStock(account, stock)
                        .orElseThrow(() ->
                                new TradingException(
                                        "POSITION_NOT_FOUND",
                                        "You do not own this stock"));

        if (portfolio.getQuantity() < quantity) {
            throw new TradingException(
                    "INSUFFICIENT_QUANTITY",
                    "You do not own enough shares to complete this order.");
        }

        BigDecimal tradeValue =
                stock.getCurrentPrice()
                        .multiply(BigDecimal.valueOf(quantity));

        BigDecimal brokerage =
                tradeValue.multiply(new BigDecimal("0.001"));

        BigDecimal amountReceived =
                tradeValue.subtract(brokerage);

        account.setCashBalance(
                account.getCashBalance().add(amountReceived));

        accountRepository.save(account);

        portfolio.setQuantity(
                portfolio.getQuantity() - quantity
        );

        Portfolio savedPortfolio;
        if (portfolio.getQuantity() == 0) {
            portfolioRepository.deleteByAccountAndStock(account, stock);
            savedPortfolio = portfolio;
        } else {
            savedPortfolio = portfolioRepository.save(portfolio);
        }

        Transaction transaction = new Transaction(
                account,
                stock,
                TransactionType.SELL,
                quantity,
                stock.getCurrentPrice(),
                tradeValue,
                brokerage,
                amountReceived,
                LocalDateTime.now()
        );

        transactionRepository.save(transaction);

        return savedPortfolio;
    }
}