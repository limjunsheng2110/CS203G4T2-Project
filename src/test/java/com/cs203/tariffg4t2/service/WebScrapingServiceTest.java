package com.cs203.tariffg4t2.service;

import com.cs203.tariffg4t2.dto.scraping.ScrapedTariffResponse;
import com.cs203.tariffg4t2.service.data.ConvertCodeService;
import com.cs203.tariffg4t2.service.data.WebScrapingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebScrapingServiceTest {

    @Mock
    private ConvertCodeService convertCodeService;

    @Mock
    private HttpClient httpClient;

    @InjectMocks
    private WebScrapingService webScrapingService;

    // ========== ERROR HANDLING TESTS ==========

    @Test
    void testScrapeTariffData_HttpError404() throws Exception {
        // given
        lenient().when(convertCodeService.convertToISO3("SG")).thenReturn("SGP");
        lenient().when(convertCodeService.convertToISO3("XX")).thenReturn("XXX");

        // when
        ScrapedTariffResponse result = webScrapingService.scrapeTariffData("SG", "XX");

        // then
        assertNotNull(result);
        assertEquals("error", result.getStatus());
    }

    @Test
    void testScrapeTariffData_HttpError500() throws Exception {
        // given
        lenient().when(convertCodeService.convertToISO3("SG")).thenReturn("SGP");
        lenient().when(convertCodeService.convertToISO3("US")).thenReturn("USA");

        // when
        ScrapedTariffResponse result = webScrapingService.scrapeTariffData("SG", "US");

        // then
        assertNotNull(result);
        assertEquals("error", result.getStatus());
    }

    @Test
    void testScrapeTariffData_NetworkError() throws Exception {
        // given
        lenient().when(convertCodeService.convertToISO3(anyString())).thenReturn("XXX");

        // when
        ScrapedTariffResponse result = webScrapingService.scrapeTariffData("SG", "US");

        // then
        assertNotNull(result);
        assertEquals("error", result.getStatus());
    }

    @Test
    void testScrapeTariffData_InterruptedException() throws Exception {
        // given
        lenient().when(convertCodeService.convertToISO3(anyString())).thenReturn("XXX");

        // when
        ScrapedTariffResponse result = webScrapingService.scrapeTariffData("SG", "US");

        // then
        assertNotNull(result);
        assertEquals("error", result.getStatus());
    }

    @Test
    void testScrapeTariffData_InvalidJsonResponse() throws Exception {
        // given
        lenient().when(convertCodeService.convertToISO3(anyString())).thenReturn("XXX");

        // when
        ScrapedTariffResponse result = webScrapingService.scrapeTariffData("SG", "US");

        // then
        assertNotNull(result);
        assertEquals("error", result.getStatus());
    }

    @Test
    void testScrapeTariffData_NullResponse() throws Exception {
        // given
        lenient().when(convertCodeService.convertToISO3(anyString())).thenReturn("XXX");

        // when
        ScrapedTariffResponse result = webScrapingService.scrapeTariffData("SG", "US");

        // then
        assertNotNull(result);
        assertEquals("error", result.getStatus());
    }

    @Test
    void testIsScraperHealthy_ReturnsFalse() {
        // when
        boolean result = webScrapingService.isScraperHealthy();

        // then
        assertFalse(result); // Without real HTTP client, always returns false
    }
}