package com.gireesh.brokerage.repository;

import com.gireesh.brokerage.entity.Transaction;
import com.gireesh.brokerage.entity.TradingAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByTradingAccountOrderByTransactionTimeDesc(TradingAccount account);
}