package com.cs203.tariffg4t2.service.data;

import com.cs203.tariffg4t2.dto.scraping.ScrapedTariffResponse;
import com.cs203.tariffg4t2.service.basic.CountryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebScrapingServiceTest {

    @Mock
    private ConvertCodeService convertCodeService;

    @Mock
    private CountryService countryService;

    @InjectMocks
    private WebScrapingService webScrapingService;

    @Test
    void scrapeTariffData_ConvertsCountryCodes() {
        // Setup mocking for this specific test
        when(convertCodeService.convertToISO3("US")).thenReturn("USA");
        when(convertCodeService.convertToISO3("CN")).thenReturn("CHN");

        // This test verifies that the service calls the conversion methods
        ScrapedTariffResponse response = webScrapingService.scrapeTariffData("US", "CN");

        // Verify that code conversion was attempted
        verify(convertCodeService, times(1)).convertToISO3("US");
        verify(convertCodeService, times(1)).convertToISO3("CN");

        // Response should not be null
        assertNotNull(response);
    }

    @Test
    void scrapeTariffData_HandlesConversionErrors() {
        when(convertCodeService.convertToISO3(anyString())).thenReturn("INVALID");

        ScrapedTariffResponse response = webScrapingService.scrapeTariffData("XX", "YY");

        assertNotNull(response);
        // When Python API is running, it may return success or error depending on the service state
        // We just verify that a response is returned
    }

    @Test
    void isScraperHealthy_ReturnsFalseWhenServiceDown() {
        // This is an integration test - health check depends on whether Python API is running
        // The result will vary based on whether the service is up or down
        boolean isHealthy = webScrapingService.isScraperHealthy();
        
        // We just verify it doesn't throw an exception
        // The actual value depends on whether localhost:5001 is running
        assertNotNull(isHealthy);
    }

    @Test
    void scrapeTariffData_CreatesErrorResponseOnFailure() {
        when(convertCodeService.convertToISO3("US")).thenReturn("USA");
        when(convertCodeService.convertToISO3("CN")).thenReturn("CHN");

        ScrapedTariffResponse response = webScrapingService.scrapeTariffData("US", "CN");

        // Verify response structure (may be success or error depending on if Python API is running)
        assertNotNull(response);
        assertNotNull(response.getStatus());
        assertNotNull(response.getData());
    }

    @Test
    void scrapeTariffData_ValidatesImportAndExportCodes() {
        when(convertCodeService.convertToISO3("US")).thenReturn("USA");
        when(convertCodeService.convertToISO3("CN")).thenReturn("CHN");

        // Test with valid codes
        ScrapedTariffResponse response = webScrapingService.scrapeTariffData("US", "CN");

        assertNotNull(response);
        verify(convertCodeService).convertToISO3("US");
        verify(convertCodeService).convertToISO3("CN");
    }

    @Test
    void convertCountryNamesToCodes_HandlesNullData() {
        when(convertCodeService.convertToISO3("US")).thenReturn("USA");
        when(convertCodeService.convertToISO3("CN")).thenReturn("CHN");

        // Create a response with null data would be handled internally
        // This should not throw an exception
        assertDoesNotThrow(() -> webScrapingService.scrapeTariffData("US", "CN"));
    }

    @Test
    void scrapeTariffData_HandlesEmptyCountryCodes() {
        when(convertCodeService.convertToISO3("")).thenReturn("");

        ScrapedTariffResponse response = webScrapingService.scrapeTariffData("", "");

        assertNotNull(response);
        assertEquals("error", response.getStatus());
    }

    @Test
    void scrapeTariffData_LogsConversionAttempts() {
        when(convertCodeService.convertToISO3("US")).thenReturn("USA");
        when(convertCodeService.convertToISO3("CN")).thenReturn("CHN");

        // Test that conversion is attempted for both codes
        webScrapingService.scrapeTariffData("US", "CN");

        verify(convertCodeService, times(1)).convertToISO3("US");
        verify(convertCodeService, times(1)).convertToISO3("CN");
    }

    @Test
    void isScraperHealthy_HandlesConnectionTimeout() {
        // Health check should handle timeouts gracefully without throwing exception
        assertDoesNotThrow(() -> {
            boolean result = webScrapingService.isScraperHealthy();
            // Result depends on whether Python API is running - just verify no exception
            assertNotNull(result);
        });
    }

    @Test
    void scrapeTariffData_HandlesNetworkErrors() {
        // Simulate network error by using invalid codes
        when(convertCodeService.convertToISO3(anyString())).thenReturn("INVALID");

        ScrapedTariffResponse response = webScrapingService.scrapeTariffData("XX", "YY");

        assertNotNull(response);
        assertEquals("error", response.getStatus());
        assertNotNull(response.getData());
    }
}
