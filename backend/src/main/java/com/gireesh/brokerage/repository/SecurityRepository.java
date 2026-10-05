package com.gireesh.brokerage.repository;

import com.gireesh.brokerage.entity.Security;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SecurityRepository
        extends JpaRepository<Security, Long>, JpaSpecificationExecutor<Security> {

    Optional<Security> findBySymbolIgnoreCase(String symbol);

    boolean existsBySymbolIgnoreCase(String symbol);
}
