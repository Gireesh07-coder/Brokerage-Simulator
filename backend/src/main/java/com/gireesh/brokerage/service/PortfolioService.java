package com.gireesh.brokerage.service;

import com.gireesh.brokerage.entity.Account;
import com.gireesh.brokerage.entity.Portfolio;
import com.gireesh.brokerage.exception.AccountNotFoundException;
import com.gireesh.brokerage.repository.AccountRepository;
import com.gireesh.brokerage.repository.PortfolioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PortfolioService {

    private final AccountRepository accountRepository;
    private final PortfolioRepository portfolioRepository;

    public PortfolioService(
            AccountRepository accountRepository,
            PortfolioRepository portfolioRepository) {

        this.accountRepository = accountRepository;
        this.portfolioRepository = portfolioRepository;
    }

    public List<Portfolio> getPortfolio(Long accountId) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(accountId));

        return portfolioRepository.findByAccount(account);
    }
}