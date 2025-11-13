package com.cs203.tariffg4t2.controller;

import com.cs203.tariffg4t2.dto.scraping.ScrapedTariffData;
import com.cs203.tariffg4t2.dto.scraping.ScrapedTariffResponse;
import com.cs203.tariffg4t2.service.data.WebScrapingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ScrapingController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        com.cs203.tariffg4t2.security.SecurityConfig.class,
                        com.cs203.tariffg4t2.security.JwtAuthenticationFilter.class,
                        com.cs203.tariffg4t2.security.JwtService.class
                }
        ))
class ScrapingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WebScrapingService webScrapingService;

    private ScrapedTariffResponse successResponse;
    private ScrapedTariffResponse errorResponse;

    @BeforeEach
    void setUp() {
        List<ScrapedTariffData> tariffData = new ArrayList<>();
        tariffData.add(new ScrapedTariffData("CN", "US", "Live cattle", "0102", "5.5%", "2024"));
        tariffData.add(new ScrapedTariffData("CN", "US", "Live sheep", "0104", "3.2%", "2024"));

        successResponse = new ScrapedTariffResponse();
        successResponse.setStatus("success");
        successResponse.setSource_url("https://wits.worldbank.org/tariff/trains/en/country/USA/partner/CHN/product/all");
        successResponse.setChapter("01");
        successResponse.setResults_count(2);
        successResponse.setData(tariffData);

        errorResponse = new ScrapedTariffResponse();
        errorResponse.setStatus("error");
        errorResponse.setSource_url("https://wits.worldbank.org/tariff/trains/en/country/USA/partner/CHN/product/all");
        errorResponse.setChapter("01");
        errorResponse.setResults_count(0);
        errorResponse.setData(new ArrayList<>());
    }

    @Test
    @WithMockUser
    void scrapeTariffData_ValidRequest_ReturnsSuccess() throws Exception {
        when(webScrapingService.scrapeTariffData("US", "CN")).thenReturn(successResponse);

        mockMvc.perform(post("/api/scraping/tariff")
                        .with(csrf())
                        .param("importCode", "US")
                        .param("exportCode", "CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.results_count").value(2))
                .andExpect(jsonPath("$.data[0].productName").value("Live cattle"));

        verify(webScrapingService, times(1)).scrapeTariffData("US", "CN");
    }

    @Test
    @WithMockUser
    void scrapeTariffData_EmptyImportCode_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/scraping/tariff")
                        .with(csrf())
                        .param("importCode", "")
                        .param("exportCode", "CN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Import code is required"));

        verify(webScrapingService, never()).scrapeTariffData(anyString(), anyString());
    }

    @Test
    @WithMockUser
    void scrapeTariffData_NullImportCode_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/scraping/tariff")
                        .with(csrf())
                        .param("exportCode", "CN"))
                .andExpect(status().isBadRequest());

        verify(webScrapingService, never()).scrapeTariffData(anyString(), anyString());
    }

    @Test
    @WithMockUser
    void scrapeTariffData_EmptyExportCode_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/scraping/tariff")
                        .with(csrf())
                        .param("importCode", "US")
                        .param("exportCode", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Export code is required"));

        verify(webScrapingService, never()).scrapeTariffData(anyString(), anyString());
    }

    @Test
    @WithMockUser
    void scrapeTariffData_InvalidImportCodeLength_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/scraping/tariff")
                        .with(csrf())
                        .param("importCode", "U")
                        .param("exportCode", "CN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Import code must be 2-3 characters"));

        verify(webScrapingService, never()).scrapeTariffData(anyString(), anyString());
    }

    @Test
    @WithMockUser
    void scrapeTariffData_InvalidExportCodeLength_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/scraping/tariff")
                        .with(csrf())
                        .param("importCode", "US")
                        .param("exportCode", "CHIN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Export code must be 2-3 characters"));

        verify(webScrapingService, never()).scrapeTariffData(anyString(), anyString());
    }

    @Test
    @WithMockUser
    void scrapeTariffData_ServiceReturnsError_ReturnsInternalServerError() throws Exception {
        when(webScrapingService.scrapeTariffData("US", "CN")).thenReturn(errorResponse);

        mockMvc.perform(post("/api/scraping/tariff")
                        .with(csrf())
                        .param("importCode", "US")
                        .param("exportCode", "CN"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"));

        verify(webScrapingService, times(1)).scrapeTariffData("US", "CN");
    }

    @Test
    @WithMockUser
    void scrapeTariffData_ServiceThrowsException_ReturnsInternalServerError() throws Exception {
        when(webScrapingService.scrapeTariffData(anyString(), anyString()))
                .thenThrow(new RuntimeException("Connection failed"));

        mockMvc.perform(post("/api/scraping/tariff")
                        .with(csrf())
                        .param("importCode", "US")
                        .param("exportCode", "CN"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Internal server error: Connection failed"));
    }

    @Test
    @WithMockUser
    void scrapeTariffData_TrimsAndUpperCasesCodes() throws Exception {
        // The controller trims and uppercases the input before calling the service
        when(webScrapingService.scrapeTariffData("US", "CN")).thenReturn(successResponse);

        mockMvc.perform(post("/api/scraping/tariff")
                        .with(csrf())
                        .param("importCode", " us ")
                        .param("exportCode", " cn "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(webScrapingService, times(1)).scrapeTariffData("US", "CN");
    }

    @Test
    @WithMockUser
    void checkScraperHealth_Healthy_ReturnsOk() throws Exception {
        when(webScrapingService.isScraperHealthy()).thenReturn(true);

        mockMvc.perform(get("/api/scraping/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scraper_status").value("healthy"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(webScrapingService, times(1)).isScraperHealthy();
    }

    @Test
    @WithMockUser
    void checkScraperHealth_Unhealthy_ReturnsServiceUnavailable() throws Exception {
        when(webScrapingService.isScraperHealthy()).thenReturn(false);

        mockMvc.perform(get("/api/scraping/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.scraper_status").value("unhealthy"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(webScrapingService, times(1)).isScraperHealthy();
    }

    @Test
    @WithMockUser
    void checkScraperHealth_ThrowsException_ReturnsInternalServerError() throws Exception {
        when(webScrapingService.isScraperHealthy()).thenThrow(new RuntimeException("Network error"));

        mockMvc.perform(get("/api/scraping/health"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Health check failed: Network error"));
    }

    @Test
    @WithMockUser
    void getScrapingStatus_ReturnsStatus() throws Exception {
        mockMvc.perform(get("/api/scraping/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("Tariff Scraping Service"))
                .andExpect(jsonPath("$.version").value("1.0"))
                .andExpect(jsonPath("$.chapter_focus").value("Chapter 1 - Live Animals"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
