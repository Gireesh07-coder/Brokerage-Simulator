package com.gireesh.brokerage.controller;

import com.gireesh.brokerage.entity.Portfolio;
import com.gireesh.brokerage.service.PortfolioService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/account/{accountId}")
    public List<Portfolio> getPortfolio(
            @PathVariable Long accountId) {

        return portfolioService.getPortfolio(accountId);
    }
}