package com.gireesh.brokerage.repository;

import com.gireesh.brokerage.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, Long> {}
