package com.gireesh.brokerage.service;

import com.gireesh.brokerage.entity.TradingAccount;
import com.gireesh.brokerage.entity.User;
import com.gireesh.brokerage.exception.ApiException;
import com.gireesh.brokerage.repository.TradingAccountRepository;
import com.gireesh.brokerage.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {
    private final UserRepository userRepository;
    private final TradingAccountRepository tradingAccountRepository;

    public CurrentUserService(
            UserRepository userRepository,
            TradingAccountRepository tradingAccountRepository) {
        this.userRepository = userRepository;
        this.tradingAccountRepository = tradingAccountRepository;
    }

    public Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required");
        }
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required");
        }
    }

    @Transactional(readOnly = true)
    public User currentUser() {
        return userRepository.findById(currentUserId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required"));
    }

    @Transactional(readOnly = true)
    public TradingAccount currentTradingAccount() {
        return tradingAccountRepository.findByUserId(currentUserId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Trading account not found"));
    }
}
