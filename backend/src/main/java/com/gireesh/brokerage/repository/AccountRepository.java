package com.gireesh.brokerage.repository;

import com.gireesh.brokerage.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}