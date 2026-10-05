package com.gireesh.brokerage.repository;

import com.gireesh.brokerage.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> { }
