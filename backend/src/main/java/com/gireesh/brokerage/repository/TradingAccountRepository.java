package com.gireesh.brokerage.repository;

import com.gireesh.brokerage.entity.TradingAccount;
import com.gireesh.brokerage.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TradingAccountRepository extends JpaRepository<TradingAccount, Long> {
    Optional<TradingAccount> findByUser(User user);

    Optional<TradingAccount> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from TradingAccount account where account.user.id = :userId")
    Optional<TradingAccount> findByUserIdForUpdate(@Param("userId") Long userId);

    // The no-op write also serializes the row in H2, which does not enforce FOR UPDATE in this test mode.
    @Modifying
    @Query(value = "update trading_accounts set updated_at = updated_at where user_id = :userId",
            nativeQuery = true)
    int acquireWriteLockForUser(@Param("userId") Long userId);
}
