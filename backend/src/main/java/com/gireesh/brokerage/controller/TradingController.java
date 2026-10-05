package com.gireesh.brokerage.controller;

import com.gireesh.brokerage.entity.Portfolio;
import com.gireesh.brokerage.service.TradingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trading")
public class TradingController {

    private final TradingService tradingService;

    public TradingController(TradingService tradingService) {
        this.tradingService = tradingService;
    }

    @PostMapping("/buy")
    public Portfolio buyStock(
            @RequestParam Long accountId,
            @RequestParam Long stockId,
            @RequestParam Integer quantity) {

        return tradingService.buyStock(
                accountId,
                stockId,
                quantity
        );
    }

    @PostMapping("/sell")
    public Portfolio sellStock(
            @RequestParam Long accountId,
            @RequestParam Long stockId,
            @RequestParam Integer quantity) {

        return tradingService.sellStock(
                accountId,
                stockId,
                quantity
        );
    }
}