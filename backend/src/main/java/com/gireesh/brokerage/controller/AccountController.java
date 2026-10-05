package com.gireesh.brokerage.controller;

import com.gireesh.brokerage.dto.TradingAccountResponse;
import com.gireesh.brokerage.service.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final CurrentUserService currentUserService;

    public AccountController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public TradingAccountResponse getCurrentAccount() {
        return TradingAccountResponse.from(currentUserService.currentTradingAccount());
    }
}
