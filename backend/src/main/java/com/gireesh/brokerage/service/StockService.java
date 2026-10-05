package com.gireesh.brokerage.service;

import com.gireesh.brokerage.entity.Stock;
import com.gireesh.brokerage.exception.StockNotFoundException;
import com.gireesh.brokerage.exception.TradingException;
import com.gireesh.brokerage.repository.StockRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class StockService {

    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public Stock createStock(
            String symbol,
            String companyName,
            BigDecimal currentPrice) {

        if (symbol == null || symbol.isBlank()) {
            throw new TradingException(
                    "INVALID_SYMBOL", "Stock symbol must not be blank");
        }

        String normalizedSymbol = symbol.trim().toUpperCase();
        if (stockRepository.existsBySymbolIgnoreCase(normalizedSymbol)) {
            throw new TradingException(
                    "DUPLICATE_SYMBOL",
                    "A stock with symbol " + normalizedSymbol + " already exists");
        }

        Stock stock = new Stock(normalizedSymbol, companyName, currentPrice);

        return stockRepository.save(stock);
    }

    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }

    public Stock getStock(Long id) {
        return stockRepository.findById(id)
                .orElseThrow(() -> new StockNotFoundException(id));
    }
}