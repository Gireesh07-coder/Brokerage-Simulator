package com.gireesh.brokerage;

import com.gireesh.brokerage.entity.Stock;
import com.gireesh.brokerage.repository.StockRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeStocks(StockRepository stockRepository) {
        return args -> {

            if (stockRepository.count() == 0) {

                stockRepository.save(
                        new Stock(
                                "AAPL",
                                "Apple Inc.",
                                new BigDecimal("230.50")
                        )
                );

                stockRepository.save(
                        new Stock(
                                "MSFT",
                                "Microsoft Corporation",
                                new BigDecimal("510.20")
                        )
                );

                stockRepository.save(
                        new Stock(
                                "TSLA",
                                "Tesla",
                                new BigDecimal("350.10")
                        )
                );

                stockRepository.save(
                        new Stock(
                                "AMZN",
                                "Amazon",
                                new BigDecimal("225.40")
                        )
                );

                stockRepository.save(
                        new Stock(
                                "GOOGL",
                                "Alphabet Inc.",
                                new BigDecimal("285.30")
                        )
                );
            }
        };
    }
}