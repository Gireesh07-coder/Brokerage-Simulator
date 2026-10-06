package com.gireesh.brokerage.repository;

import com.gireesh.brokerage.entity.Position;
import com.gireesh.brokerage.entity.Security;
import com.gireesh.brokerage.entity.TradingAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {
    Optional<Position> findByAccountAndSecurity(TradingAccount account, Security security);

    List<Position> findByAccountOrderBySecuritySymbolAsc(TradingAccount account);

    Page<Position> findByAccountOrderBySecuritySymbolAsc(TradingAccount account, Pageable pageable);
}
