package com.gireesh.brokerage.repository;

import com.gireesh.brokerage.entity.Account;
import com.gireesh.brokerage.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountOrderByTransactionTimeDesc(
            Account account
    );
}