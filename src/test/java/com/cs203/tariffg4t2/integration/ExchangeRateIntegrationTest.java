package com.cs203.tariffg4t2.integration;

import com.cs203.tariffg4t2.dto.request.ExchangeRateAnalysisRequest;
import com.cs203.tariffg4t2.dto.response.ExchangeRateAnalysisResponse;
import com.cs203.tariffg4t2.model.basic.Country;
import com.cs203.tariffg4t2.model.basic.ExchangeRate;
import com.cs203.tariffg4t2.repository.basic.CountryRepository;
import com.cs203.tariffg4t2.repository.basic.ExchangeRateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
    "openexchangerates.api.key=test_key",
    "openexchangerates.api.url=https://test.api.url",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=password",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ExchangeRateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private CountryRepository countryRepository;

    @BeforeEach
    void setUp() {
        // Clean up existing data
        exchangeRateRepository.deleteAll();
        
        // Ensure test countries exist with proper ISO3 codes
        if (!countryRepository.existsById("US")) {
            countryRepository.save(new Country("US", "United States", "USA"));
        }
        if (!countryRepository.existsById("CN")) {
            countryRepository.save(new Country("CN", "China", "CHN"));
        }
        if (!countryRepository.existsById("GB")) {
            countryRepository.save(new Country("GB", "United Kingdom", "GBR"));
        }
        if (!countryRepository.existsById("JP")) {
            countryRepository.save(new Country("JP", "Japan", "JPN"));
        }

        // Force flush to ensure data is persisted
        countryRepository.flush();

        // Create test exchange rate data
        createTestExchangeRates();
    }

    private void createTestExchangeRates() {
        List<ExchangeRate> rates = new ArrayList<>();
        LocalDate startDate = LocalDate.now().minusMonths(6);

        // Create historical data for CNY -> USD
        for (int i = 0; i < 180; i += 7) {
            ExchangeRate rate = new ExchangeRate();
            rate.setFromCurrency("CNY");
            rate.setToCurrency("USD");
            rate.setRate(new BigDecimal("0.138").add(new BigDecimal(Math.random() * 0.01)));
            rate.setRateDate(startDate.plusDays(i));
            rates.add(rate);
        }

        // Create historical data for GBP -> USD
        for (int i = 0; i < 180; i += 7) {
            ExchangeRate rate = new ExchangeRate();
            rate.setFromCurrency("GBP");
            rate.setToCurrency("USD");
            rate.setRate(new BigDecimal("1.25").add(new BigDecimal(Math.random() * 0.05)));
            rate.setRateDate(startDate.plusDays(i));
            rates.add(rate);
        }

        // Create current rates
        ExchangeRate currentCnyUsd = new ExchangeRate();
        currentCnyUsd.setFromCurrency("CNY");
        currentCnyUsd.setToCurrency("USD");
        currentCnyUsd.setRate(new BigDecimal("0.1385"));
        currentCnyUsd.setRateDate(LocalDate.now());
        rates.add(currentCnyUsd);

        ExchangeRate currentGbpUsd = new ExchangeRate();
        currentGbpUsd.setFromCurrency("GBP");
        currentGbpUsd.setToCurrency("USD");
        currentGbpUsd.setRate(new BigDecimal("1.27"));
        currentGbpUsd.setRateDate(LocalDate.now());
        rates.add(currentGbpUsd);

        exchangeRateRepository.saveAll(rates);
    }

    @Test
    void testExchangeRateAnalysisEndpoint_POST_Success() throws Exception {
        // Arrange
        ExchangeRateAnalysisRequest request = ExchangeRateAnalysisRequest.builder()
            .importingCountry("US")
            .exportingCountry("CN")
            .build();

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/exchange-rates/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importingCountry", is("US")))
                .andExpect(jsonPath("$.exportingCountry", is("CN")))
                .andExpect(jsonPath("$.importingCurrency", is("USD")))
                .andExpect(jsonPath("$.exportingCurrency", is("CNY")))
                .andExpect(jsonPath("$.currentRate", notNullValue()))
                .andExpect(jsonPath("$.averageRate", notNullValue()))
                .andExpect(jsonPath("$.minRate", notNullValue()))
                .andExpect(jsonPath("$.maxRate", notNullValue()))
                .andExpect(jsonPath("$.recommendedPurchaseDate", notNullValue()))
                .andExpect(jsonPath("$.recommendation", notNullValue()))
                .andExpect(jsonPath("$.trendAnalysis", notNullValue()))
                .andExpect(jsonPath("$.historicalRates", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.dataSource", notNullValue()))
                .andReturn();

        // Parse response and verify data structure
        String responseJson = result.getResponse().getContentAsString();
        ExchangeRateAnalysisResponse response = objectMapper.readValue(responseJson, ExchangeRateAnalysisResponse.class);
        
        assertNotNull(response.getCurrentRate());
        assertTrue(response.getHistoricalRates().size() > 0);
        assertTrue(response.getMinRate().compareTo(response.getMaxRate()) <= 0);
    }

    @Test
    void testExchangeRateAnalysisEndpoint_GET_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/exchange-rates/analyze")
                .param("importingCountry", "US")
                .param("exportingCountry", "CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importingCountry", is("US")))
                .andExpect(jsonPath("$.exportingCountry", is("CN")))
                .andExpect(jsonPath("$.currentRate", notNullValue()));
    }

    @Test
    void testExchangeRateAnalysisEndpoint_InvalidCountry() throws Exception {
        // Arrange
        ExchangeRateAnalysisRequest request = ExchangeRateAnalysisRequest.builder()
            .importingCountry("INVALID")
            .exportingCountry("CN")
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/exchange-rates/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    @Test
    void testExchangeRateAnalysisEndpoint_MissingCountry() throws Exception {
        // Arrange
        ExchangeRateAnalysisRequest request = ExchangeRateAnalysisRequest.builder()
            .importingCountry("")
            .exportingCountry("CN")
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/exchange-rates/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testExchangeRateAnalysisEndpoint_Alpha3CountryCodes() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/exchange-rates/analyze")
                .param("importingCountry", "USA")
                .param("exportingCountry", "CHN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importingCurrency", is("USD")))
                .andExpect(jsonPath("$.exportingCurrency", is("CNY")));
    }

    @Test
    void testExchangeRateAnalysisEndpoint_DifferentCurrencyPair() throws Exception {
        // Test with GBP -> USD
        mockMvc.perform(get("/api/exchange-rates/analyze")
                .param("importingCountry", "US")
                .param("exportingCountry", "GB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importingCountry", is("US")))
                .andExpect(jsonPath("$.exportingCountry", is("GB")))
                .andExpect(jsonPath("$.importingCurrency", is("USD")))
                .andExpect(jsonPath("$.exportingCurrency", is("GBP")))
                .andExpect(jsonPath("$.currentRate", notNullValue()));
    }

    @Test
    void testHealthCheckEndpoint() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/exchange-rates/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.service", is("exchange-rate-service")))
                .andExpect(jsonPath("$.apiReference", is("https://openexchangerates.org/")));
    }

    @Test
    void testApiReferenceEndpoint() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/exchange-rates/api-reference"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider", is("OpenExchangeRates")))
                .andExpect(jsonPath("$.url", is("https://openexchangerates.org/")))
                .andExpect(jsonPath("$.description", notNullValue()));
    }

    @Test
    void testFullPipeline_InputToOutput() throws Exception {
        // This test simulates the full user flow:
        // 1. User selects countries
        // 2. System analyzes exchange rates
        // 3. System provides recommendation

        // Step 1: Verify countries exist
        assertTrue(countryRepository.existsById("US"));
        assertTrue(countryRepository.existsById("CN"));

        // Step 2: Request analysis
        ExchangeRateAnalysisRequest request = ExchangeRateAnalysisRequest.builder()
            .importingCountry("US")
            .exportingCountry("CN")
            .build();

        MvcResult result = mockMvc.perform(post("/api/exchange-rates/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Step 3: Verify complete response structure
        String responseJson = result.getResponse().getContentAsString();
        ExchangeRateAnalysisResponse response = objectMapper.readValue(responseJson, ExchangeRateAnalysisResponse.class);

        // Verify all required fields are present
        assertNotNull(response.getImportingCountry());
        assertNotNull(response.getExportingCountry());
        assertNotNull(response.getImportingCurrency());
        assertNotNull(response.getExportingCurrency());
        assertNotNull(response.getCurrentRate());
        assertNotNull(response.getCurrentRateDate());
        assertNotNull(response.getAverageRate());
        assertNotNull(response.getMinRate());
        assertNotNull(response.getMinRateDate());
        assertNotNull(response.getMaxRate());
        assertNotNull(response.getMaxRateDate());
        assertNotNull(response.getRecommendedPurchaseDate());
        assertNotNull(response.getRecommendation());
        assertNotNull(response.getTrendAnalysis());
        assertNotNull(response.getHistoricalRates());
        assertNotNull(response.getDataSource());

        // Verify data validity
        assertTrue(response.getMinRate().compareTo(response.getMaxRate()) <= 0);
        assertTrue(response.getHistoricalRates().size() > 0);
        assertTrue(List.of("increasing", "decreasing", "stable").contains(response.getTrendAnalysis()));
        
        // Verify recommendation contains meaningful text
        assertTrue(response.getRecommendation().length() > 10);
    }
}
