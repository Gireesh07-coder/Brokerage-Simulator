package com.gireesh.brokerage.controller;

import com.gireesh.brokerage.entity.Stock;
import com.gireesh.brokerage.service.StockService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Stock createStock(
            @RequestParam String symbol,
            @RequestParam String companyName,
            @RequestParam BigDecimal currentPrice) {

        return stockService.createStock(
                symbol,
                companyName,
                currentPrice
        );
    }

    @GetMapping
    public List<Stock> getAllStocks() {
        return stockService.getAllStocks();
    }

    @GetMapping("/{id}")
    public Stock getStock(@PathVariable Long id) {
        return stockService.getStock(id);
    }
}