package com.gireesh.brokerage;

import com.gireesh.brokerage.entity.Security;
import com.gireesh.brokerage.entity.Role;
import com.gireesh.brokerage.entity.TradingAccount;
import com.gireesh.brokerage.entity.User;
import com.gireesh.brokerage.repository.SecurityRepository;
import com.gireesh.brokerage.repository.TradingAccountRepository;
import com.gireesh.brokerage.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class DataInitializer {
    private record SeedSecurity(
            String symbol,
            String companyName,
            String sector,
            String price,
            String previousClose) {
    }

    @Bean
    CommandLineRunner initializeSecurities(SecurityRepository securityRepository) {
        return args -> {
            LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
            for (SeedSecurity seed : seedSecurities()) {
                if (!securityRepository.existsBySymbolIgnoreCase(seed.symbol())) {
                    Security security = new Security();
                    security.setSymbol(seed.symbol());
                    security.setCompanyName(seed.companyName());
                    security.setExchange("NSE");
                    security.setSector(seed.sector());
                    security.setCurrentPrice(new BigDecimal(seed.price()));
                    security.setPreviousClose(new BigDecimal(seed.previousClose()));
                    security.setVolume(0L);
                    security.setSimulated(true);
                    security.setCreatedAt(now);
                    security.setUpdatedAt(now);
                    securityRepository.save(security);
                }
            }
        };
    }

    @Bean
    CommandLineRunner initializeAdmin(
            UserRepository userRepository,
            TradingAccountRepository tradingAccountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword,
            @Value("${app.account.starting-cash:100000.00}") BigDecimal startingCash) {
        return args -> {
            if (adminEmail.isBlank() && adminPassword.isBlank()) {
                return;
            }
            if (adminEmail.isBlank() || adminPassword.isBlank()) {
                throw new IllegalStateException(
                        "ADMIN_EMAIL and ADMIN_PASSWORD must either both be set or both be blank");
            }
            String normalizedEmail = adminEmail.trim().toLowerCase(java.util.Locale.ROOT);
            if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
                return;
            }
            if (adminPassword.length() < 8
                    || !adminPassword.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
                throw new IllegalStateException(
                        "ADMIN_PASSWORD must be at least 8 characters and include a letter and a digit");
            }

            User admin = new User(
                    normalizedEmail,
                    passwordEncoder.encode(adminPassword),
                    "Administrator");
            admin.setRole(Role.ADMIN);
            User savedAdmin = userRepository.saveAndFlush(admin);
            tradingAccountRepository.save(new TradingAccount(savedAdmin, startingCash));
        };
    }

    private List<SeedSecurity> seedSecurities() {
        return List.of(
                new SeedSecurity("RELIANCE", "Reliance Industries Ltd.", "Energy", "1450.00", "1442.80"),
                new SeedSecurity("TCS", "Tata Consultancy Services Ltd.", "Information Technology", "3250.00", "3266.25"),
                new SeedSecurity("HDFCBANK", "HDFC Bank Ltd.", "Financial Services", "980.00", "975.10"),
                new SeedSecurity("ICICIBANK", "ICICI Bank Ltd.", "Financial Services", "1360.00", "1353.20"),
                new SeedSecurity("INFY", "Infosys Ltd.", "Information Technology", "1520.00", "1512.40"),
                new SeedSecurity("ITC", "ITC Ltd.", "Consumer Goods", "415.00", "412.93"),
                new SeedSecurity("SBIN", "State Bank of India", "Financial Services", "820.00", "815.90"),
                new SeedSecurity("BHARTIARTL", "Bharti Airtel Ltd.", "Telecommunication", "1890.00", "1880.55"),
                new SeedSecurity("LT", "Larsen & Toubro Ltd.", "Construction", "3580.00", "3562.10"),
                new SeedSecurity("HINDUNILVR", "Hindustan Unilever Ltd.", "Consumer Goods", "2380.00", "2368.10"),
                new SeedSecurity("KOTAKBANK", "Kotak Mahindra Bank Ltd.", "Financial Services", "1780.00", "1771.10"),
                new SeedSecurity("AXISBANK", "Axis Bank Ltd.", "Financial Services", "1120.00", "1114.40"),
                new SeedSecurity("MARUTI", "Maruti Suzuki India Ltd.", "Automobile", "12400.00", "12338.00"),
                new SeedSecurity("SUNPHARMA", "Sun Pharmaceutical Industries Ltd.", "Healthcare", "1720.00", "1711.40"),
                new SeedSecurity("TATAMOTORS", "Tata Motors Ltd.", "Automobile", "680.00", "676.60"),
                new SeedSecurity("TATASTEEL", "Tata Steel Ltd.", "Metals", "155.00", "154.23"),
                new SeedSecurity("NTPC", "NTPC Ltd.", "Utilities", "365.00", "363.18"),
                new SeedSecurity("POWERGRID", "Power Grid Corporation of India Ltd.", "Utilities", "325.00", "323.38"),
                new SeedSecurity("BAJFINANCE", "Bajaj Finance Ltd.", "Financial Services", "7200.00", "7164.00"),
                new SeedSecurity("ASIANPAINT", "Asian Paints Ltd.", "Consumer Goods", "2850.00", "2835.75")
        );
    }
}
