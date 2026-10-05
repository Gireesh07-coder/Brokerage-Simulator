package com.gireesh.brokerage.repository;

import com.gireesh.brokerage.entity.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {
}
