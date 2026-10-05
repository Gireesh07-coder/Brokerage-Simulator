package com.gireesh.brokerage.repository;

import com.gireesh.brokerage.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> { }
