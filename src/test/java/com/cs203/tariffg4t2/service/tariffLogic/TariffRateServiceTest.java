package com.cs203.tariffg4t2.service.tariffLogic;

import com.cs203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.cs203.tariffg4t2.dto.scraping.ScrapedTariffData;
import com.cs203.tariffg4t2.dto.scraping.ScrapedTariffResponse;
import com.cs203.tariffg4t2.model.basic.TariffRate;
import com.cs203.tariffg4t2.repository.basic.TariffRateRepository;
import com.cs203.tariffg4t2.service.basic.TariffRateCRUDService;
import com.cs203.tariffg4t2.service.data.WebScrapingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TariffRateServiceTest {

    @Mock
    private WebScrapingService webScrapingService;

    @Mock
    private TariffRateCRUDService tariffRateCRUDService;

    @Mock
    private TariffRateRepository tariffRateRepository;

    @InjectMocks
    private TariffRateService tariffRateService;

    private TariffCalculationRequestDTO testRequest;
    private TariffRate testTariffRate;

    @BeforeEach
    void setUp() {
        testRequest = new TariffCalculationRequestDTO();
        testRequest.setHsCode("123456");
        testRequest.setImportingCountry("US");
        testRequest.setExportingCountry("CN");
        testRequest.setProductValue(new BigDecimal("1000"));
        testRequest.setYear(2024);

        testTariffRate = new TariffRate();
        testTariffRate.setId(1L);
        testTariffRate.setHsCode("123456");
        testTariffRate.setImportingCountryCode("US");
        testTariffRate.setExportingCountryCode("CN");
        testTariffRate.setAdValoremRate(new BigDecimal("7.5"));
        testTariffRate.setYear(2024);
    }

    @Test
    void calculateTariffAmount_WithValidRate_CalculatesCorrectly() {
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                "123456", "US", "CN", 2024)).thenReturn(Optional.of(testTariffRate));

        BigDecimal result = tariffRateService.calculateTariffAmount(testRequest);

        assertEquals(new BigDecimal("75.00"), result); // 1000 * 7.5 / 100
    }

    @Test
    void calculateTariffAmount_NoRateFound_TriggersWebScraping() {
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                "123456", "US", "CN", 2024)).thenReturn(Optional.empty());
        when(tariffRateRepository.findClosestYearTariffRate("123456", "US", "CN", 2024))
                .thenReturn(new ArrayList<>());
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "123456", "US", "CN")).thenReturn(new ArrayList<>());

        ScrapedTariffResponse scrapedResponse = new ScrapedTariffResponse();
        scrapedResponse.setStatus("success");
        scrapedResponse.setResults_count(1);

        ScrapedTariffData scrapedData = new ScrapedTariffData();
        scrapedData.setHsCode("123456");
        scrapedData.setImportingCountry("US");
        scrapedData.setExportingCountry("CN");
        scrapedData.setTariffRate("7.5%");
        scrapedData.setDate("2024");

        scrapedResponse.setData(Arrays.asList(scrapedData));

        when(webScrapingService.scrapeTariffData("US", "CN")).thenReturn(scrapedResponse);

        TariffRate savedRate = new TariffRate();
        savedRate.setId(1L);
        savedRate.setHsCode("123456");
        savedRate.setImportingCountryCode("US");
        savedRate.setExportingCountryCode("CN");
        savedRate.setAdValoremRate(new BigDecimal("7.5"));
        savedRate.setYear(2024);
        when(tariffRateRepository.save(any(TariffRate.class))).thenReturn(savedRate);

        BigDecimal result = tariffRateService.calculateTariffAmount(testRequest);

        assertEquals(0, result.compareTo(BigDecimal.ZERO)); // Compare using compareTo instead of equals
        verify(webScrapingService, times(1)).scrapeTariffData("US", "CN");
    }

    @Test
    void calculateTariffAmount_WebScrapingFails_ReturnsZero() {
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                "123456", "US", "CN", 2024)).thenReturn(Optional.empty());
        when(tariffRateRepository.findClosestYearTariffRate("123456", "US", "CN", 2024))
                .thenReturn(new ArrayList<>());
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "123456", "US", "CN")).thenReturn(new ArrayList<>());
        when(webScrapingService.scrapeTariffData("US", "CN"))
                .thenThrow(new RuntimeException("Scraping failed"));

        BigDecimal result = tariffRateService.calculateTariffAmount(testRequest);

        assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    void calculateTariffAmount_NullAdValoremRate_ReturnsZero() {
        testTariffRate.setAdValoremRate(null);
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                "123456", "US", "CN", 2024)).thenReturn(Optional.of(testTariffRate));

        BigDecimal result = tariffRateService.calculateTariffAmount(testRequest);

        assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    void calculateTariffAmount_ZeroAdValoremRate_ReturnsZero() {
        testTariffRate.setAdValoremRate(BigDecimal.ZERO);
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                "123456", "US", "CN", 2024)).thenReturn(Optional.of(testTariffRate));

        BigDecimal result = tariffRateService.calculateTariffAmount(testRequest);

        assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    void calculateTariffAmount_NullProductValue_UsesZero() {
        testRequest.setProductValue(null);
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                "123456", "US", "CN", 2024)).thenReturn(Optional.of(testTariffRate));

        BigDecimal result = tariffRateService.calculateTariffAmount(testRequest);

        assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    void calculateTariffAmount_HighPrecisionRate_RoundsCorrectly() {
        testTariffRate.setAdValoremRate(new BigDecimal("7.555"));
        testRequest.setProductValue(new BigDecimal("1000"));
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                "123456", "US", "CN", 2024)).thenReturn(Optional.of(testTariffRate));

        BigDecimal result = tariffRateService.calculateTariffAmount(testRequest);

        assertEquals(new BigDecimal("75.55"), result);
    }

    @Test
    void calculateTariffAmount_WebScrapingReturnsNoData_ReturnsZero() {
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                "123456", "US", "CN", 2024)).thenReturn(Optional.empty());
        when(tariffRateRepository.findClosestYearTariffRate("123456", "US", "CN", 2024))
                .thenReturn(new ArrayList<>());
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "123456", "US", "CN")).thenReturn(new ArrayList<>());

        ScrapedTariffResponse scrapedResponse = new ScrapedTariffResponse();
        scrapedResponse.setStatus("error");
        scrapedResponse.setData(null);

        when(webScrapingService.scrapeTariffData("US", "CN")).thenReturn(scrapedResponse);

        BigDecimal result = tariffRateService.calculateTariffAmount(testRequest);

        assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    void calculateTariffAmount_WebScrapingSuccessButNoMatchingHS_ReturnsZero() {
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                "123456", "US", "CN", 2024)).thenReturn(Optional.empty());
        when(tariffRateRepository.findClosestYearTariffRate("123456", "US", "CN", 2024))
                .thenReturn(new ArrayList<>());
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "123456", "US", "CN")).thenReturn(new ArrayList<>());

        ScrapedTariffResponse scrapedResponse = new ScrapedTariffResponse();
        scrapedResponse.setStatus("success");
        scrapedResponse.setResults_count(1);

        ScrapedTariffData scrapedData = new ScrapedTariffData();
        scrapedData.setHsCode("999999"); // Different HS code
        scrapedData.setImportingCountry("US");
        scrapedData.setExportingCountry("CN");
        scrapedData.setTariffRate("5.0%");
        scrapedData.setDate("2024");

        scrapedResponse.setData(Arrays.asList(scrapedData));

        when(webScrapingService.scrapeTariffData("US", "CN")).thenReturn(scrapedResponse);
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "999999", "US", "CN")).thenReturn(new ArrayList<>());

        TariffRate savedRate = new TariffRate();
        savedRate.setId(2L);
        when(tariffRateRepository.save(any(TariffRate.class))).thenReturn(savedRate);

        BigDecimal result = tariffRateService.calculateTariffAmount(testRequest);

        assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    void calculateTariffAmount_NullYear_UsesMethodWithoutYear() {
        testRequest.setYear(null);
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "123456", "US", "CN")).thenReturn(Arrays.asList(testTariffRate));

        BigDecimal result = tariffRateService.calculateTariffAmount(testRequest);

        assertEquals(new BigDecimal("75.00"), result);
    }

    @Test
    void getTariffRateWithYear_ExactYearMatch_ReturnsRate() {
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                "123456", "US", "CN", 2024)).thenReturn(Optional.of(testTariffRate));

        Optional<TariffRate> result = tariffRateService.getTariffRateWithYear("123456", "US", "CN", 2024);

        assertTrue(result.isPresent());
        assertEquals(2024, result.get().getYear());
    }

    @Test
    void getTariffRateWithYear_NullYear_ReturnsAnyYear() {
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "123456", "US", "CN")).thenReturn(Arrays.asList(testTariffRate));

        Optional<TariffRate> result = tariffRateService.getTariffRateWithYear("123456", "US", "CN", null);

        assertTrue(result.isPresent());
    }

    @Test
    void getTariffRateWithYear_NotFound_ReturnsEmpty() {
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                "999999", "JP", "KR", 2024)).thenReturn(Optional.empty());
        when(tariffRateRepository.findClosestYearTariffRate("999999", "JP", "KR", 2024))
                .thenReturn(new ArrayList<>());
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "999999", "JP", "KR")).thenReturn(new ArrayList<>());

        Optional<TariffRate> result = tariffRateService.getTariffRateWithYear("999999", "JP", "KR", 2024);

        assertFalse(result.isPresent());
    }

    @Test
    void saveScrapedData_DuplicateRate_SkipsSave() {
        ScrapedTariffData scrapedData = new ScrapedTariffData();
        scrapedData.setHsCode("123456");
        scrapedData.setImportingCountry("US");
        scrapedData.setExportingCountry("CN");
        scrapedData.setTariffRate("7.5%");
        scrapedData.setDate("2024");

        // Setup: no rate found initially, triggers web scraping
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                "123456", "US", "CN", 2024)).thenReturn(Optional.empty());
        when(tariffRateRepository.findClosestYearTariffRate("123456", "US", "CN", 2024))
                .thenReturn(new ArrayList<>());
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "123456", "US", "CN")).thenReturn(new ArrayList<>())  // First call during initial lookup
                .thenReturn(Arrays.asList(testTariffRate))  // Second call during save finds duplicate
                .thenReturn(Arrays.asList(testTariffRate));  // Third call after scraping

        ScrapedTariffResponse scrapedResponse = new ScrapedTariffResponse();
        scrapedResponse.setStatus("success");
        scrapedResponse.setResults_count(1);
        scrapedResponse.setData(Arrays.asList(scrapedData));

        when(webScrapingService.scrapeTariffData("US", "CN")).thenReturn(scrapedResponse);

        tariffRateService.calculateTariffAmount(testRequest);

        verify(tariffRateRepository, times(3)).findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "123456", "US", "CN");
        verify(tariffRateRepository, never()).save(any(TariffRate.class));
    }

    @Test
    void parseYearFromDate_ValidYear2024_ReturnsYear() {
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                anyString(), anyString(), anyString(), anyInt())).thenReturn(Optional.empty());
        when(tariffRateRepository.findClosestYearTariffRate(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(new ArrayList<>());
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                anyString(), anyString(), anyString())).thenReturn(new ArrayList<>());

        ScrapedTariffData scrapedData = new ScrapedTariffData();
        scrapedData.setHsCode("123456");
        scrapedData.setImportingCountry("US");
        scrapedData.setExportingCountry("CN");
        scrapedData.setTariffRate("7.5%");
        scrapedData.setDate("2024-01-15"); // Date with year

        ScrapedTariffResponse scrapedResponse = new ScrapedTariffResponse();
        scrapedResponse.setStatus("success");
        scrapedResponse.setResults_count(1);
        scrapedResponse.setData(Arrays.asList(scrapedData));

        when(webScrapingService.scrapeTariffData("US", "CN")).thenReturn(scrapedResponse);

        TariffRate savedRate = new TariffRate();
        savedRate.setId(1L);
        savedRate.setYear(2024);
        when(tariffRateRepository.save(any(TariffRate.class))).thenReturn(savedRate);

        tariffRateService.calculateTariffAmount(testRequest);

        verify(tariffRateRepository).save(argThat(rate ->
            rate.getYear() != null && rate.getYear() == 2024
        ));
    }

    @Test
    void parseYearFromDate_InvalidDateString_UsesRequestedYear() {
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                anyString(), anyString(), anyString(), anyInt())).thenReturn(Optional.empty());
        when(tariffRateRepository.findClosestYearTariffRate(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(new ArrayList<>());
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                anyString(), anyString(), anyString())).thenReturn(new ArrayList<>());

        ScrapedTariffData scrapedData = new ScrapedTariffData();
        scrapedData.setHsCode("123456");
        scrapedData.setImportingCountry("US");
        scrapedData.setExportingCountry("CN");
        scrapedData.setTariffRate("7.5%");
        scrapedData.setDate("invalid-date"); // Invalid date

        ScrapedTariffResponse scrapedResponse = new ScrapedTariffResponse();
        scrapedResponse.setStatus("success");
        scrapedResponse.setResults_count(1);
        scrapedResponse.setData(Arrays.asList(scrapedData));

        when(webScrapingService.scrapeTariffData("US", "CN")).thenReturn(scrapedResponse);

        TariffRate savedRate = new TariffRate();
        savedRate.setId(1L);
        savedRate.setYear(2024);
        when(tariffRateRepository.save(any(TariffRate.class))).thenReturn(savedRate);

        tariffRateService.calculateTariffAmount(testRequest);

        verify(tariffRateRepository).save(argThat(rate ->
            rate.getYear() != null && rate.getYear() == 2024
        ));
    }

    @Test
    void parseTariffRate_ValidPercentage_ParsesCorrectly() {
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                anyString(), anyString(), anyString(), anyInt())).thenReturn(Optional.empty());
        when(tariffRateRepository.findClosestYearTariffRate(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(new ArrayList<>());
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                anyString(), anyString(), anyString())).thenReturn(new ArrayList<>());

        ScrapedTariffData scrapedData = new ScrapedTariffData();
        scrapedData.setHsCode("123456");
        scrapedData.setImportingCountry("US");
        scrapedData.setExportingCountry("CN");
        scrapedData.setTariffRate("15.25%"); // High precision rate
        scrapedData.setDate("2024");

        ScrapedTariffResponse scrapedResponse = new ScrapedTariffResponse();
        scrapedResponse.setStatus("success");
        scrapedResponse.setResults_count(1);
        scrapedResponse.setData(Arrays.asList(scrapedData));

        when(webScrapingService.scrapeTariffData("US", "CN")).thenReturn(scrapedResponse);

        TariffRate savedRate = new TariffRate();
        savedRate.setId(1L);
        when(tariffRateRepository.save(any(TariffRate.class))).thenReturn(savedRate);

        tariffRateService.calculateTariffAmount(testRequest);

        verify(tariffRateRepository).save(argThat(rate ->
            rate.getAdValoremRate() != null &&
            rate.getAdValoremRate().compareTo(new BigDecimal("15.25")) == 0
        ));
    }

    @Test
    void parseTariffRate_InvalidRate_DefaultsToZero() {
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                anyString(), anyString(), anyString(), anyInt())).thenReturn(Optional.empty());
        when(tariffRateRepository.findClosestYearTariffRate(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(new ArrayList<>());
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                anyString(), anyString(), anyString())).thenReturn(new ArrayList<>());

        ScrapedTariffData scrapedData = new ScrapedTariffData();
        scrapedData.setHsCode("123456");
        scrapedData.setImportingCountry("US");
        scrapedData.setExportingCountry("CN");
        scrapedData.setTariffRate("invalid%"); // Invalid rate
        scrapedData.setDate("2024");

        ScrapedTariffResponse scrapedResponse = new ScrapedTariffResponse();
        scrapedResponse.setStatus("success");
        scrapedResponse.setResults_count(1);
        scrapedResponse.setData(Arrays.asList(scrapedData));

        when(webScrapingService.scrapeTariffData("US", "CN")).thenReturn(scrapedResponse);

        TariffRate savedRate = new TariffRate();
        savedRate.setId(1L);
        when(tariffRateRepository.save(any(TariffRate.class))).thenReturn(savedRate);

        tariffRateService.calculateTariffAmount(testRequest);

        verify(tariffRateRepository).save(argThat(rate ->
            rate.getAdValoremRate() != null &&
            rate.getAdValoremRate().compareTo(BigDecimal.ZERO) == 0
        ));
    }

    @Test
    void saveScrapedData_ExceptionDuringSave_ContinuesWithOthers() {
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                anyString(), anyString(), anyString(), anyInt())).thenReturn(Optional.empty());
        when(tariffRateRepository.findClosestYearTariffRate(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(new ArrayList<>());
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                anyString(), anyString(), anyString())).thenReturn(new ArrayList<>());

        ScrapedTariffData scrapedData1 = new ScrapedTariffData();
        scrapedData1.setHsCode("123456");
        scrapedData1.setImportingCountry("US");
        scrapedData1.setExportingCountry("CN");
        scrapedData1.setTariffRate("7.5%");
        scrapedData1.setDate("2024");

        ScrapedTariffData scrapedData2 = new ScrapedTariffData();
        scrapedData2.setHsCode("789012");
        scrapedData2.setImportingCountry("US");
        scrapedData2.setExportingCountry("CN");
        scrapedData2.setTariffRate("10.0%");
        scrapedData2.setDate("2024");

        ScrapedTariffResponse scrapedResponse = new ScrapedTariffResponse();
        scrapedResponse.setStatus("success");
        scrapedResponse.setResults_count(2);
        scrapedResponse.setData(Arrays.asList(scrapedData1, scrapedData2));

        when(webScrapingService.scrapeTariffData("US", "CN")).thenReturn(scrapedResponse);

        TariffRate savedRate = new TariffRate();
        savedRate.setId(1L);

        // First save throws exception, second should still proceed
        when(tariffRateRepository.save(any(TariffRate.class)))
                .thenThrow(new RuntimeException("Database error"))
                .thenReturn(savedRate);

        tariffRateService.calculateTariffAmount(testRequest);

        // Verify both were attempted to be saved
        verify(tariffRateRepository, times(2)).save(any(TariffRate.class));
    }
}
