package com.gireesh.brokerage.repository;

import com.gireesh.brokerage.entity.Transaction;
import com.gireesh.brokerage.entity.TradingAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByTradingAccountOrderByTransactionTimeDesc(TradingAccount account);

    Page<Transaction> findByTradingAccountOrderByTransactionTimeDesc(
            TradingAccount account, Pageable pageable);

    Optional<Transaction> findByOrderId(Long orderId);
}