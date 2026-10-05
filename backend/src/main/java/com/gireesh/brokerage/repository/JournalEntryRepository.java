package com.gireesh.brokerage.repository;

import com.gireesh.brokerage.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> { }
