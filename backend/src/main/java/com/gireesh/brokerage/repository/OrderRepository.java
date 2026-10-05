package com.gireesh.brokerage.repository;

import com.gireesh.brokerage.entity.Order;
import com.gireesh.brokerage.entity.TradingAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByAccountOrderByCreatedAtDesc(TradingAccount account, Pageable pageable);
}
