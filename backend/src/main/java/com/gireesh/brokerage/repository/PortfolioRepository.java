package com.gireesh.brokerage.repository;

import com.gireesh.brokerage.entity.Account;
import com.gireesh.brokerage.entity.Portfolio;
import com.gireesh.brokerage.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    Optional<Portfolio> findByAccountAndStock(Account account, Stock stock);

    List<Portfolio> findByAccount(Account account);

    void deleteByAccountAndStock(Account account, Stock stock);
}