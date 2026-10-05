package com.gireesh.brokerage.repository;

import com.gireesh.brokerage.entity.TradePlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradePlanRepository extends JpaRepository<TradePlan, Long> { }
