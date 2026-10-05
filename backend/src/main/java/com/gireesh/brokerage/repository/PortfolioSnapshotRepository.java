package com.gireesh.brokerage.repository;

import com.gireesh.brokerage.entity.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> { }
