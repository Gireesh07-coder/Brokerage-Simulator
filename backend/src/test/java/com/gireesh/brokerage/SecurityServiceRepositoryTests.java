package com.gireesh.brokerage;

import com.gireesh.brokerage.dto.PageResponse;
import com.gireesh.brokerage.dto.SecurityResponse;
import com.gireesh.brokerage.entity.Security;
import com.gireesh.brokerage.repository.SecurityRepository;
import com.gireesh.brokerage.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class SecurityServiceRepositoryTests {
    @Autowired
    private SecurityRepository securityRepository;

    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        securityService = new SecurityService(securityRepository);
        securityRepository.deleteAll();
        securityRepository.saveAndFlush(security("HDFCBANK", "HDFC Bank Ltd.", "1600.00"));
        securityRepository.saveAndFlush(security("ICICIBANK", "ICICI Bank Ltd.", "1200.00"));
        securityRepository.saveAndFlush(
                security("WIPRO", "Wipro Ltd.", "500.00", "Information Technology"));
    }

    @Test
    void searchFiltersBySymbolOrCompanySectorAndReturnsRequestedPage() {
        PageResponse<SecurityResponse> firstPage = securityService.search(
                "bank", "Financial Services", "currentPrice", "desc", 0, 1);
        PageResponse<SecurityResponse> secondPage = securityService.search(
                "bank", "Financial Services", "currentPrice", "desc", 1, 1);

        assertThat(firstPage.totalElements()).isEqualTo(2);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.content()).extracting(SecurityResponse::symbol)
                .containsExactly("HDFCBANK");
        assertThat(secondPage.content()).extracting(SecurityResponse::symbol)
                .containsExactly("ICICIBANK");
    }

    private static Security security(String symbol, String name, String price) {
        return security(symbol, name, price, "Financial Services");
    }

    private static Security security(String symbol, String name, String price, String sector) {
        Security security = new Security();
        security.setSymbol(symbol);
        security.setCompanyName(name);
        security.setCurrentPrice(new BigDecimal(price));
        security.setPreviousClose(new BigDecimal(price));
        security.setExchange("NSE");
        security.setSector(sector);
        security.setSimulated(true);
        return security;
    }
}
