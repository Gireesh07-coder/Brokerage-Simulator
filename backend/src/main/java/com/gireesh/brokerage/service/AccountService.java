package com.gireesh.brokerage.service;

import com.gireesh.brokerage.entity.Account;
import com.gireesh.brokerage.exception.AccountNotFoundException;
import com.gireesh.brokerage.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account createAccount(String name, BigDecimal initialBalance) {
        Account account = new Account(name, initialBalance);
        return accountRepository.save(account);
    }

    public Account getAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }
}