package com.gireesh.brokerage;

import com.gireesh.brokerage.controller.SecurityController;
import com.gireesh.brokerage.dto.SecurityResponse;
import com.gireesh.brokerage.entity.Security;
import com.gireesh.brokerage.exception.ApiException;
import com.gireesh.brokerage.exception.GlobalExceptionHandler;
import com.gireesh.brokerage.repository.SecurityRepository;
import com.gireesh.brokerage.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PhaseTwoBehaviorTests {
    @Mock
    private SecurityRepository securityRepository;

    private SecurityService securityService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        securityService = new SecurityService(securityRepository);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SecurityController(securityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void securitiesEndpointReturnsPagedDtosAndSearchMetadata() throws Exception {
        Security security = security(1L, "TCS", "Tata Consultancy Services Ltd.");
        when(securityRepository.findAll(
                ArgumentMatchers.<Specification<Security>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(security),
                        PageRequest.of(0, 20),
                        1));

        mockMvc.perform(get("/api/securities")
                        .param("q", "Tata")
                        .param("sector", "Information Technology")
                        .param("sort", "symbol")
                        .param("direction", "asc")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].symbol").value("TCS"))
                .andExpect(jsonPath("$.content[0].simulated").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void securityDetailEndpointAcceptsSymbolCaseInsensitively() throws Exception {
        Security security = security(7L, "TCS", "Tata Consultancy Services Ltd.");
        when(securityRepository.findBySymbolIgnoreCase("tcs"))
                .thenReturn(Optional.of(security));

        mockMvc.perform(get("/api/securities/tcs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.symbol").value("TCS"));
    }

    @Test
    void missingSecurityReturnsStandardErrorShape() throws Exception {
        when(securityRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/securities/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("SECURITY_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Security not found: 999"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void searchBuildsPaginationAndSortRequest() {
        when(securityRepository.findAll(
                ArgumentMatchers.<Specification<Security>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        securityService.search("TCS", "Information Technology", "symbol", "desc", 2, 10);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(securityRepository).findAll(
                ArgumentMatchers.<Specification<Security>>any(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort().getOrderFor("symbol").getDirection().name())
                .isEqualTo("DESC");
    }

    @Test
    void invalidPageSizeIsRejected() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> securityService.search(null, null, "symbol", "asc", 0, 101));

        assertThat(exception.getErrorCode()).isEqualTo("INVALID_PAGE_SIZE");
    }

    @Test
    void unsupportedSortFieldIsRejected() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> securityService.search(null, null, "passwordHash", "asc", 0, 20));

        assertThat(exception.getErrorCode()).isEqualTo("INVALID_SORT");
    }

    private static Security security(Long id, String symbol, String companyName) {
        Security security = mock(Security.class);
        when(security.getId()).thenReturn(id);
        when(security.getSymbol()).thenReturn(symbol);
        when(security.getCompanyName()).thenReturn(companyName);
        when(security.getCurrentPrice()).thenReturn(new BigDecimal("3250.00"));
        when(security.getExchange()).thenReturn("NSE");
        when(security.getSector()).thenReturn("Information Technology");
        when(security.getPreviousClose()).thenReturn(new BigDecimal("3233.75"));
        when(security.isSimulated()).thenReturn(true);
        return security;
    }
}
