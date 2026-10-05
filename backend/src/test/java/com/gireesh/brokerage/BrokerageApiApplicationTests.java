package com.gireesh.brokerage;

import com.gireesh.brokerage.repository.SecurityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BrokerageApiApplicationTests {
    @Autowired
    private SecurityRepository securityRepository;

    @Test
    void contextLoadsAndInitializesIndianSecuritiesIdempotently() {
        org.assertj.core.api.Assertions.assertThat(securityRepository.count()).isGreaterThanOrEqualTo(20);
        org.assertj.core.api.Assertions.assertThat(
                securityRepository.findBySymbolIgnoreCase("RELIANCE"))
                .isPresent()
                .get()
                .extracting(security -> security.getExchange())
                .isEqualTo("NSE");
    }

}
