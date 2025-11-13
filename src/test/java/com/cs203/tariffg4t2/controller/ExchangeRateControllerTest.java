package com.cs203.tariffg4t2.controller;

import com.cs203.tariffg4t2.dto.request.ExchangeRateAnalysisRequest;
import com.cs203.tariffg4t2.dto.response.ExchangeRateAnalysisResponse;
import com.cs203.tariffg4t2.service.basic.ExchangeRateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ExchangeRateController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        com.cs203.tariffg4t2.security.SecurityConfig.class,
                        com.cs203.tariffg4t2.security.JwtAuthenticationFilter.class,
                        com.cs203.tariffg4t2.security.JwtService.class
                }
        ))
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExchangeRateService exchangeRateService;

    private ExchangeRateAnalysisRequest validRequest;
    private ExchangeRateAnalysisResponse validResponse;

    @BeforeEach
    void setUp() {
        validRequest = ExchangeRateAnalysisRequest.builder()
                .importingCountry("US")
                .exportingCountry("CN")
                .build();

        validResponse = ExchangeRateAnalysisResponse.builder()
                .importingCountry("US")
                .exportingCountry("CN")
                .importingCurrency("USD")
                .exportingCurrency("CNY")
                .currentRate(new BigDecimal("6.5"))
                .averageRate(new BigDecimal("6.45"))
                .minRate(new BigDecimal("6.3"))
                .historicalRates(new ArrayList<>())
                .recommendedPurchaseDate(LocalDate.now())
                .recommendation("Good time to purchase")
                .liveDataAvailable(true)
                .dataSource("live_api")
                .build();
    }

    @Test
    @WithMockUser
    void analyzeExchangeRates_ValidRequest_ReturnsSuccess() throws Exception {
        when(exchangeRateService.analyzeExchangeRates(any(ExchangeRateAnalysisRequest.class)))
                .thenReturn(validResponse);

        mockMvc.perform(post("/api/exchange-rates/analyze")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importingCountry").value("US"))
                .andExpect(jsonPath("$.exportingCountry").value("CN"))
                .andExpect(jsonPath("$.currentRate").value(6.5));

        verify(exchangeRateService, times(1)).analyzeExchangeRates(any(ExchangeRateAnalysisRequest.class));
    }

    @Test
    @WithMockUser
    void analyzeExchangeRates_InvalidCountryCode_ReturnsBadRequest() throws Exception {
        when(exchangeRateService.analyzeExchangeRates(any(ExchangeRateAnalysisRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid country code"));

        mockMvc.perform(post("/api/exchange-rates/analyze")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.details").value("Invalid country code"));
    }

    @Test
    @WithMockUser
    void analyzeExchangeRates_ServiceError_ReturnsInternalServerError() throws Exception {
        when(exchangeRateService.analyzeExchangeRates(any(ExchangeRateAnalysisRequest.class)))
                .thenThrow(new RuntimeException("API connection failed"));

        mockMvc.perform(post("/api/exchange-rates/analyze")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.details").value("API connection failed"));
    }

    @Test
    @WithMockUser
    void analyzeExchangeRatesGet_ValidRequest_ReturnsSuccess() throws Exception {
        when(exchangeRateService.analyzeExchangeRates(any(ExchangeRateAnalysisRequest.class)))
                .thenReturn(validResponse);

        mockMvc.perform(get("/api/exchange-rates/analyze")
                        .param("importingCountry", "US")
                        .param("exportingCountry", "CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importingCountry").value("US"))
                .andExpect(jsonPath("$.exportingCountry").value("CN"));
    }

    @Test
    @WithMockUser
    void analyzeExchangeRatesGet_InvalidCountryCode_ReturnsBadRequest() throws Exception {
        when(exchangeRateService.analyzeExchangeRates(any(ExchangeRateAnalysisRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid country code"));

        mockMvc.perform(get("/api/exchange-rates/analyze")
                        .param("importingCountry", "XX")
                        .param("exportingCountry", "YY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser
    void healthCheck_ReturnsHealthyStatus() throws Exception {
        mockMvc.perform(get("/api/exchange-rates/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("exchange-rate-service"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.apiReference").value("https://openexchangerates.org/"));
    }

    @Test
    @WithMockUser
    void getApiReference_ReturnsApiDetails() throws Exception {
        mockMvc.perform(get("/api/exchange-rates/api-reference"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("OpenExchangeRates"))
                .andExpect(jsonPath("$.url").value("https://openexchangerates.org/"))
                .andExpect(jsonPath("$.description").exists());
    }
}
