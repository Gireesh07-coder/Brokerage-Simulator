package com.gireesh.brokerage.repository;

import com.gireesh.brokerage.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> { }
