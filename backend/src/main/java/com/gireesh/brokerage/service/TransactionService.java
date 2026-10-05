package com.gireesh.brokerage.service;

import com.gireesh.brokerage.entity.Account;
import com.gireesh.brokerage.entity.Transaction;
import com.gireesh.brokerage.exception.AccountNotFoundException;
import com.gireesh.brokerage.repository.AccountRepository;
import com.gireesh.brokerage.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository) {

        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> getTransactions(Long accountId) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(accountId));

        return transactionRepository
                .findByAccountOrderByTransactionTimeDesc(account);
    }
}