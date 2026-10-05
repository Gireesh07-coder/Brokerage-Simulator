package com.gireesh.brokerage;

import com.gireesh.brokerage.entity.Stock;
import com.gireesh.brokerage.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
class StockConstraintTests {

    @Autowired
    private StockRepository stockRepository;

    @Test
    void duplicateStockSymbolsAreRejectedByDatabaseConstraint() {
        stockRepository.saveAndFlush(
                new Stock("TEST", "Test Security", new BigDecimal("10.00")));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> stockRepository.saveAndFlush(
                        new Stock("TEST", "Another Security", new BigDecimal("20.00"))));
    }
}
