package com.gireesh.brokerage.repository;

import com.gireesh.brokerage.entity.TradingAccount;
import com.gireesh.brokerage.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TradingAccountRepository extends JpaRepository<TradingAccount, Long> {
    Optional<TradingAccount> findByUser(User user);
}
