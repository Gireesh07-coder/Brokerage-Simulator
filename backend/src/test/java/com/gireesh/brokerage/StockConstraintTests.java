package com.gireesh.brokerage;

import com.gireesh.brokerage.entity.Position;
import com.gireesh.brokerage.entity.Security;
import com.gireesh.brokerage.entity.TradingAccount;
import com.gireesh.brokerage.entity.User;
import com.gireesh.brokerage.repository.PositionRepository;
import com.gireesh.brokerage.repository.SecurityRepository;
import com.gireesh.brokerage.repository.TradingAccountRepository;
import com.gireesh.brokerage.repository.UserRepository;
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
    private SecurityRepository securityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TradingAccountRepository tradingAccountRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Test
    void duplicateSecuritySymbolsAreRejectedByDatabaseConstraint() {
        securityRepository.saveAndFlush(security("TEST"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> securityRepository.saveAndFlush(security("TEST")));
    }

    @Test
    void negativeTradingAccountCashIsRejectedByDatabaseConstraint() {
        User user = userRepository.saveAndFlush(
                new User("negative-cash@example.test", "hash", "Negative Cash"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> tradingAccountRepository.saveAndFlush(
                        new TradingAccount(user, new BigDecimal("-0.01"))));
    }

    @Test
    void negativePositionQuantityIsRejectedByDatabaseConstraint() {
        User user = userRepository.saveAndFlush(
                new User("negative-position@example.test", "hash", "Negative Position"));
        TradingAccount account = tradingAccountRepository.saveAndFlush(
                new TradingAccount(user, new BigDecimal("1000.00")));
        Security security = securityRepository.saveAndFlush(security("POSITION"));
        Position position = new Position();
        position.setAccount(account);
        position.setSecurity(security);
        position.setQuantity(-1);
        position.setAverageCost(BigDecimal.ZERO);
        position.setRealizedPnl(BigDecimal.ZERO);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> positionRepository.saveAndFlush(position));
    }

    private static Security security(String symbol) {
        Security security = new Security();
        security.setSymbol(symbol);
        security.setCompanyName("Test Security");
        security.setCurrentPrice(new BigDecimal("10.00"));
        security.setExchange("NSE");
        security.setSector("Testing");
        security.setPreviousClose(new BigDecimal("10.00"));
        security.setSimulated(true);
        return security;
    }
}
