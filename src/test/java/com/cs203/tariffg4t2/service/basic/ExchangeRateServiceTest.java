package com.cs203.tariffg4t2.service.basic;

import com.cs203.tariffg4t2.dto.request.ExchangeRateAnalysisRequest;
import com.cs203.tariffg4t2.dto.response.ExchangeRateAnalysisResponse;
import com.cs203.tariffg4t2.model.basic.Country;
import com.cs203.tariffg4t2.model.basic.ExchangeRate;
import com.cs203.tariffg4t2.repository.basic.CountryRepository;
import com.cs203.tariffg4t2.repository.basic.ExchangeRateRepository;
import com.cs203.tariffg4t2.service.data.CurrencyCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private CurrencyCodeService currencyCodeService;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    private Country usaCountry;
    private Country sgCountry;
    private ExchangeRate currentRate;
    private List<ExchangeRate> historicalRates;

    @BeforeEach
    void setUp() {
        // Set up test data
        usaCountry = new Country();
        usaCountry.setCountryCode("US");
        usaCountry.setCountryName("United States");

        sgCountry = new Country();
        sgCountry.setCountryCode("SG");
        sgCountry.setCountryName("Singapore");

        currentRate = new ExchangeRate();
        currentRate.setFromCurrency("USD");
        currentRate.setToCurrency("SGD");
        currentRate.setRate(new BigDecimal("1.35"));
        currentRate.setRateDate(LocalDate.now());

        historicalRates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 180; i += 7) {
            ExchangeRate rate = new ExchangeRate();
            rate.setFromCurrency("USD");
            rate.setToCurrency("SGD");
            rate.setRate(new BigDecimal("1.30").add(new BigDecimal(i * 0.0001)));
            rate.setRateDate(today.minusDays(i));
            historicalRates.add(rate);
        }

        // Set API key via reflection for testing
        ReflectionTestUtils.setField(exchangeRateService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(exchangeRateService, "apiUrl", "https://test.api.url");
    }

    @Test
    void analyzeExchangeRates_Success() {
        // Given
        ExchangeRateAnalysisRequest request = new ExchangeRateAnalysisRequest();
        request.setImportingCountry("SG");
        request.setExportingCountry("US");

        when(countryRepository.findById("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findById("US")).thenReturn(Optional.of(usaCountry));
        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.of(currentRate));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndRateDateBetween(
                eq("USD"), eq("SGD"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(historicalRates);

        // When
        ExchangeRateAnalysisResponse response = exchangeRateService.analyzeExchangeRates(request);

        // Then
        assertNotNull(response);
        assertEquals("SG", response.getImportingCountry());
        assertEquals("US", response.getExportingCountry());
        assertEquals("SGD", response.getImportingCurrency());
        assertEquals("USD", response.getExportingCurrency());
        assertNotNull(response.getCurrentRate());
        assertNotNull(response.getAverageRate());
        assertNotNull(response.getTrendAnalysis());
    }

    @Test
    void analyzeExchangeRates_InvalidImportingCountry() {
        // Given
        ExchangeRateAnalysisRequest request = new ExchangeRateAnalysisRequest();
        request.setImportingCountry("INVALID");
        request.setExportingCountry("US");

        when(countryRepository.findById("INVALID")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                exchangeRateService.analyzeExchangeRates(request));
    }

    @Test
    void analyzeExchangeRates_InvalidExportingCountry() {
        // Given
        ExchangeRateAnalysisRequest request = new ExchangeRateAnalysisRequest();
        request.setImportingCountry("SG");
        request.setExportingCountry("INVALID");

        when(countryRepository.findById("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findById("INVALID")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                exchangeRateService.analyzeExchangeRates(request));
    }

    @Test
    void analyzeExchangeRates_CurrencyMappingNotFound() {
        // Given
        ExchangeRateAnalysisRequest request = new ExchangeRateAnalysisRequest();
        request.setImportingCountry("SG");
        request.setExportingCountry("US");

        when(countryRepository.findById("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findById("US")).thenReturn(Optional.of(usaCountry));
        when(currencyCodeService.getCurrencyCode("SG")).thenReturn(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                exchangeRateService.analyzeExchangeRates(request));
    }

    @Test
    void analyzeExchangeRates_NoExchangeRateData() {
        // Given
        ExchangeRateAnalysisRequest request = new ExchangeRateAnalysisRequest();
        request.setImportingCountry("SG");
        request.setExportingCountry("US");

        when(countryRepository.findById("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findById("US")).thenReturn(Optional.of(usaCountry));
        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () ->
                exchangeRateService.analyzeExchangeRates(request));
    }

    @Test
    void analyzeExchangeRates_WithHistoricalData() {
        // Given
        ExchangeRateAnalysisRequest request = new ExchangeRateAnalysisRequest();
        request.setImportingCountry("SG");
        request.setExportingCountry("US");

        when(countryRepository.findById("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findById("US")).thenReturn(Optional.of(usaCountry));
        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.of(currentRate));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndRateDateBetween(
                eq("USD"), eq("SGD"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(historicalRates);

        // When
        ExchangeRateAnalysisResponse response = exchangeRateService.analyzeExchangeRates(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getHistoricalRates());
        assertFalse(response.getHistoricalRates().isEmpty());
        assertNotNull(response.getMinRate());
        assertNotNull(response.getMaxRate());
        assertNotNull(response.getMinRateDate());
        assertNotNull(response.getMaxRateDate());
    }

    @Test
    void analyzeExchangeRates_EmptyHistoricalData() {
        // Given
        ExchangeRateAnalysisRequest request = new ExchangeRateAnalysisRequest();
        request.setImportingCountry("SG");
        request.setExportingCountry("US");

        when(countryRepository.findById("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findById("US")).thenReturn(Optional.of(usaCountry));
        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.of(currentRate));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndRateDateBetween(
                eq("USD"), eq("SGD"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new ArrayList<>());

        // When & Then - Should throw RuntimeException when no historical data
        assertThrows(RuntimeException.class, () ->
                exchangeRateService.analyzeExchangeRates(request));
    }

    @Test
    void analyzeExchangeRates_CountryCodeWith3Digits() {
        // Given
        ExchangeRateAnalysisRequest request = new ExchangeRateAnalysisRequest();
        request.setImportingCountry("SGP");
        request.setExportingCountry("USA");

        when(countryRepository.findById("SGP")).thenReturn(Optional.empty());
        when(countryRepository.findById("USA")).thenReturn(Optional.empty());
        when(countryRepository.findByIso3CodeIgnoreCase("SGP")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByIso3CodeIgnoreCase("USA")).thenReturn(Optional.of(usaCountry));
        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.of(currentRate));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndRateDateBetween(
                eq("USD"), eq("SGD"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(historicalRates);

        // When
        ExchangeRateAnalysisResponse response = exchangeRateService.analyzeExchangeRates(request);

        // Then
        assertNotNull(response);
    }

    @Test
    void analyzeExchangeRates_TrendAnalysisIncreasing() {
        // Given
        List<ExchangeRate> increasingRates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 180; i >= 0; i -= 7) {
            ExchangeRate rate = new ExchangeRate();
            rate.setFromCurrency("USD");
            rate.setToCurrency("SGD");
            rate.setRate(new BigDecimal("1.20").add(new BigDecimal(i * 0.001)));
            rate.setRateDate(today.minusDays(i));
            increasingRates.add(rate);
        }

        ExchangeRateAnalysisRequest request = new ExchangeRateAnalysisRequest();
        request.setImportingCountry("SG");
        request.setExportingCountry("US");

        when(countryRepository.findById("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findById("US")).thenReturn(Optional.of(usaCountry));
        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.of(currentRate));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndRateDateBetween(
                eq("USD"), eq("SGD"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(increasingRates);

        // When
        ExchangeRateAnalysisResponse response = exchangeRateService.analyzeExchangeRates(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getTrendAnalysis());
    }

    @Test
    void analyzeExchangeRates_TrendAnalysisDecreasing() {
        // Given
        List<ExchangeRate> decreasingRates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 180; i += 7) {
            ExchangeRate rate = new ExchangeRate();
            rate.setFromCurrency("USD");
            rate.setToCurrency("SGD");
            rate.setRate(new BigDecimal("1.50").subtract(new BigDecimal(i * 0.001)));
            rate.setRateDate(today.minusDays(i));
            decreasingRates.add(rate);
        }

        ExchangeRateAnalysisRequest request = new ExchangeRateAnalysisRequest();
        request.setImportingCountry("SG");
        request.setExportingCountry("US");

        when(countryRepository.findById("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findById("US")).thenReturn(Optional.of(usaCountry));
        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.of(currentRate));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndRateDateBetween(
                eq("USD"), eq("SGD"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(decreasingRates);

        // When
        ExchangeRateAnalysisResponse response = exchangeRateService.analyzeExchangeRates(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getTrendAnalysis());
    }

    @Test
    void analyzeExchangeRates_WithRecommendation() {
        // Given
        ExchangeRateAnalysisRequest request = new ExchangeRateAnalysisRequest();
        request.setImportingCountry("SG");
        request.setExportingCountry("US");

        when(countryRepository.findById("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findById("US")).thenReturn(Optional.of(usaCountry));
        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.of(currentRate));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndRateDateBetween(
                eq("USD"), eq("SGD"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(historicalRates);

        // When
        ExchangeRateAnalysisResponse response = exchangeRateService.analyzeExchangeRates(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getRecommendation());
        assertNotNull(response.getRecommendedPurchaseDate());
    }

    @Test
    void analyzeExchangeRates_DataSourceIndicator() {
        // Given
        ExchangeRateAnalysisRequest request = new ExchangeRateAnalysisRequest();
        request.setImportingCountry("SG");
        request.setExportingCountry("US");

        when(countryRepository.findById("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findById("US")).thenReturn(Optional.of(usaCountry));
        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.of(currentRate));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndRateDateBetween(
                eq("USD"), eq("SGD"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(historicalRates);

        // When
        ExchangeRateAnalysisResponse response = exchangeRateService.analyzeExchangeRates(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getDataSource());
        assertNotNull(response.getMessage());
    }
}
