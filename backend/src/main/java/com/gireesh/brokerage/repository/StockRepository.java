package com.gireesh.brokerage.repository;

import com.gireesh.brokerage.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {

    boolean existsBySymbolIgnoreCase(String symbol);
}