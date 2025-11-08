package com.cs203.tariffg4t2.service;

import com.cs203.tariffg4t2.dto.request.ExchangeRateAnalysisRequest;
import com.cs203.tariffg4t2.dto.response.ExchangeRateAnalysisResponse;
import com.cs203.tariffg4t2.model.basic.Country;
import com.cs203.tariffg4t2.model.basic.ExchangeRate;
import com.cs203.tariffg4t2.repository.basic.CountryRepository;
import com.cs203.tariffg4t2.repository.basic.ExchangeRateRepository;
import com.cs203.tariffg4t2.service.basic.ExchangeRateService;
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

    private Country usCountry;
    private Country cnCountry;
    private ExchangeRate currentRate;
    private List<ExchangeRate> historicalRates;

    @BeforeEach
    void setUp() {
        // Set up test data
        usCountry = new Country("US", "United States", "USA", null);
        cnCountry = new Country("CN", "China", "CHN", null);

        currentRate = new ExchangeRate();
        currentRate.setFromCurrency("CNY");
        currentRate.setToCurrency("USD");
        currentRate.setRate(new BigDecimal("0.1385"));
        currentRate.setRateDate(LocalDate.now());

        // Create historical data for past 6 months
        historicalRates = new ArrayList<>();
        LocalDate startDate = LocalDate.now().minusMonths(6);
        for (int i = 0; i < 180; i += 7) { // Weekly data points
            ExchangeRate rate = new ExchangeRate();
            rate.setFromCurrency("CNY");
            rate.setToCurrency("USD");
            rate.setRate(new BigDecimal("0.138").add(BigDecimal.valueOf(Math.random() * 0.01)));
            rate.setRateDate(startDate.plusDays(i));
            historicalRates.add(rate);
        }

        // Set API key for testing (even though we won't call real API)
        ReflectionTestUtils.setField(exchangeRateService, "apiKey", "test_api_key");
        ReflectionTestUtils.setField(exchangeRateService, "apiUrl", "https://test.api.url");
    }

    @Test
    void testAnalyzeExchangeRates_Success() {
        // Arrange
        ExchangeRateAnalysisRequest request = ExchangeRateAnalysisRequest.builder()
            .importingCountry("US")
            .exportingCountry("CN")
            .build();

        when(countryRepository.findById("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findById("CN")).thenReturn(Optional.of(cnCountry));
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(currencyCodeService.getCurrencyCode("CN")).thenReturn("CNY");
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("CNY", "USD"))
            .thenReturn(Optional.of(currentRate));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndRateDateBetween(
            eq("CNY"), eq("USD"), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(historicalRates);

        // Act
        ExchangeRateAnalysisResponse response = exchangeRateService.analyzeExchangeRates(request);

        // Assert
        assertNotNull(response);
        assertEquals("US", response.getImportingCountry());
        assertEquals("CN", response.getExportingCountry());
        assertEquals("USD", response.getImportingCurrency());
        assertEquals("CNY", response.getExportingCurrency());
        assertNotNull(response.getCurrentRate());
        assertNotNull(response.getAverageRate());
        assertNotNull(response.getMinRate());
        assertNotNull(response.getMaxRate());
        assertNotNull(response.getRecommendedPurchaseDate());
        assertNotNull(response.getRecommendation());
        assertNotNull(response.getTrendAnalysis());
        assertTrue(response.getHistoricalRates().isEmpty());

        // Verify interactions
        verify(countryRepository, times(1)).findById("US");
        verify(countryRepository, times(1)).findById("CN");
        verify(currencyCodeService, times(1)).getCurrencyCode("US");
        verify(currencyCodeService, times(1)).getCurrencyCode("CN");
    }

    @Test
    void testAnalyzeExchangeRates_InvalidImportingCountry() {
        // Arrange
        ExchangeRateAnalysisRequest request = ExchangeRateAnalysisRequest.builder()
            .importingCountry("INVALID")
            .exportingCountry("CN")
            .build();

        when(countryRepository.findById("INVALID")).thenReturn(Optional.empty());
        when(countryRepository.findByCountryNameIgnoreCase("INVALID")).thenReturn(Optional.empty());
        when(countryRepository.findById("CN")).thenReturn(Optional.of(cnCountry));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            exchangeRateService.analyzeExchangeRates(request));
    }

    @Test
    void testAnalyzeExchangeRates_NoCurrencyMapping() {
        // Arrange
        ExchangeRateAnalysisRequest request = ExchangeRateAnalysisRequest.builder()
            .importingCountry("US")
            .exportingCountry("CN")
            .build();

        when(countryRepository.findById("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findById("CN")).thenReturn(Optional.of(cnCountry));
        when(currencyCodeService.getCurrencyCode("US")).thenReturn(null); // No currency mapping

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            exchangeRateService.analyzeExchangeRates(request));
    }

    @Test
    void testAnalyzeExchangeRates_NoHistoricalData() {
        // Arrange
        ExchangeRateAnalysisRequest request = ExchangeRateAnalysisRequest.builder()
            .importingCountry("US")
            .exportingCountry("CN")
            .build();

        when(countryRepository.findById("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findById("CN")).thenReturn(Optional.of(cnCountry));
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(currencyCodeService.getCurrencyCode("CN")).thenReturn("CNY");
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("CNY", "USD"))
            .thenReturn(Optional.of(currentRate));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndRateDateBetween(
            eq("CNY"), eq("USD"), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(new ArrayList<>()); // Empty list

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            exchangeRateService.analyzeExchangeRates(request));
    }

    @Test
    void testAnalyzeExchangeRates_WithAlpha3CountryCode() {
        // Arrange
        ExchangeRateAnalysisRequest request = ExchangeRateAnalysisRequest.builder()
            .importingCountry("USA")
            .exportingCountry("CHN")
            .build();

        // Mock for USA (3-character lookup)
        when(countryRepository.findById("USA")).thenReturn(Optional.empty());
        when(countryRepository.findByIso3CodeIgnoreCase("USA")).thenReturn(Optional.of(usCountry));

        // Mock for CHN (3-character lookup)
        when(countryRepository.findById("CHN")).thenReturn(Optional.empty());
        when(countryRepository.findByIso3CodeIgnoreCase("CHN")).thenReturn(Optional.of(cnCountry));

        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(currencyCodeService.getCurrencyCode("CN")).thenReturn("CNY");
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("CNY", "USD"))
            .thenReturn(Optional.of(currentRate));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndRateDateBetween(
            eq("CNY"), eq("USD"), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(historicalRates);

        // Act
        ExchangeRateAnalysisResponse response = exchangeRateService.analyzeExchangeRates(request);

        // Assert
        assertNotNull(response);
        assertEquals("US", response.getImportingCountry());
        assertEquals("CN", response.getExportingCountry());
    }

    @Test
    void testAnalyzeExchangeRates_TrendAnalysis_Increasing() {
        // Arrange - Create increasing trend
        List<ExchangeRate> increasingRates = new ArrayList<>();
        LocalDate startDate = LocalDate.now().minusMonths(6);
        for (int i = 0; i < 26; i++) { // 26 weeks
            ExchangeRate rate = new ExchangeRate();
            rate.setFromCurrency("CNY");
            rate.setToCurrency("USD");
            rate.setRate(new BigDecimal("0.130").add(new BigDecimal(i * 0.001))); // Gradually increasing
            rate.setRateDate(startDate.plusWeeks(i));
            increasingRates.add(rate);
        }

        ExchangeRateAnalysisRequest request = ExchangeRateAnalysisRequest.builder()
            .importingCountry("US")
            .exportingCountry("CN")
            .build();

        when(countryRepository.findById("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findById("CN")).thenReturn(Optional.of(cnCountry));
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(currencyCodeService.getCurrencyCode("CN")).thenReturn("CNY");
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("CNY", "USD"))
            .thenReturn(Optional.of(currentRate));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndRateDateBetween(
            eq("CNY"), eq("USD"), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(increasingRates);

        // Act
        ExchangeRateAnalysisResponse response = exchangeRateService.analyzeExchangeRates(request);

        // Assert
        assertNotNull(response);
        assertEquals("increasing", response.getTrendAnalysis());
    }

    @Test
    void testAnalyzeExchangeRates_TrendAnalysis_Decreasing() {
        // Arrange - Create decreasing trend
        List<ExchangeRate> decreasingRates = new ArrayList<>();
        LocalDate startDate = LocalDate.now().minusMonths(6);
        for (int i = 0; i < 26; i++) { // 26 weeks
            ExchangeRate rate = new ExchangeRate();
            rate.setFromCurrency("CNY");
            rate.setToCurrency("USD");
            rate.setRate(new BigDecimal("0.150").subtract(new BigDecimal(i * 0.001))); // Gradually decreasing
            rate.setRateDate(startDate.plusWeeks(i));
            decreasingRates.add(rate);
        }

        ExchangeRateAnalysisRequest request = ExchangeRateAnalysisRequest.builder()
            .importingCountry("US")
            .exportingCountry("CN")
            .build();

        when(countryRepository.findById("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findById("CN")).thenReturn(Optional.of(cnCountry));
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(currencyCodeService.getCurrencyCode("CN")).thenReturn("CNY");
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("CNY", "USD"))
            .thenReturn(Optional.of(currentRate));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrencyAndRateDateBetween(
            eq("CNY"), eq("USD"), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(decreasingRates);

        // Act
        ExchangeRateAnalysisResponse response = exchangeRateService.analyzeExchangeRates(request);

        // Assert
        assertNotNull(response);
        assertEquals("decreasing", response.getTrendAnalysis());
    }
}
