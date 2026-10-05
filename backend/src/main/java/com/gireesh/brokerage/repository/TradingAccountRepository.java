package com.gireesh.brokerage.repository;

import com.gireesh.brokerage.entity.TradingAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradingAccountRepository extends JpaRepository<TradingAccount, Long> { }
