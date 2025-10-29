package com.cs203.tariffg4t2.service.data;

import com.cs203.tariffg4t2.dto.scraping.ScrapedTariffData;
import com.cs203.tariffg4t2.dto.scraping.ScrapedTariffResponse;
import com.cs203.tariffg4t2.service.basic.CountryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class WebScrapingService {

    @Autowired
    private ConvertCodeService convertCodeService;

    @Autowired
    private CountryService countryService;

    private static final Logger logger = LoggerFactory.getLogger(WebScrapingService.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String PYTHON_API_BASE_URL = "http://localhost:5001";

    public WebScrapingService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public ScrapedTariffResponse scrapeTariffData(String importCode, String exportCode) {
        // Convert to 3 digit iso-code
        String convertedImportCode = convertCodeService.convertToISO3(importCode);
        String convertedExportCode = convertCodeService.convertToISO3(exportCode);

        logger.info("Starting tariff scraping for import: {} -> {}, export: {} -> {}",
                   importCode, convertedImportCode, exportCode, convertedExportCode);

        try {
            // Prepare form data
            String formData = String.format("import_code=%s&export_code=%s",
                    URLEncoder.encode(convertedImportCode, StandardCharsets.UTF_8),
                    URLEncoder.encode(convertedExportCode, StandardCharsets.UTF_8));

            logger.debug("Sending request to Python scraper with import_code: {}, export_code: {}",
                        convertedImportCode, convertedExportCode);

            // Build HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PYTHON_API_BASE_URL + "/scrape"))
                    .timeout(Duration.ofMinutes(5))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            // Send request and get response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parse JSON response
                ScrapedTariffResponse scrapedResponse = objectMapper.readValue(response.body(), ScrapedTariffResponse.class);

                if (scrapedResponse != null) {
                    // Convert country names to 2-digit codes in the scraped data
                    convertCountryNamesToCodes(scrapedResponse);

                    logger.info("Successfully scraped {} tariff records for {}->{}",
                               scrapedResponse.getResults_count(), exportCode, importCode);
                    return scrapedResponse;
                } else {
                    logger.warn("Received null response from scraper for {}->{}", exportCode, importCode);
                    return createErrorResponse(importCode, exportCode, "Null response from scraper");
                }
            } else {
                logger.error("HTTP error during scraping for {}->{}: Status={}, Body={}",
                            exportCode, importCode, response.statusCode(), response.body());
                return createErrorResponse(importCode, exportCode, "HTTP Error: " + response.statusCode());
            }

        } catch (IOException | InterruptedException e) {
            logger.error("Network error during scraping for {}->{}: {}", convertedExportCode, convertedImportCode, e.getMessage(), e);
            return createErrorResponse(convertedImportCode, convertedExportCode, "Network error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during scraping for {}->{}: {}", convertedExportCode, convertedImportCode, e.getMessage(), e);
            return createErrorResponse(convertedImportCode, convertedExportCode, "Scraping failed: " + e.getMessage());
        }
    }

    /**
     * Convert country names to 2-digit codes in a single tariff data record
     */
    private ScrapedTariffData convertTariffDataCountryCodes(ScrapedTariffData data) {
        String originalImporting = data.getImportingCountry();
        String originalExporting = data.getExportingCountry();

        String convertedImporting = countryService.convertCountryNameToIso2(originalImporting);
        String convertedExporting = countryService.convertCountryNameToIso2(originalExporting);

        // Use converted values if conversion succeeds, otherwise keep original
        if (convertedImporting != null) {
            data.setImportingCountry(convertedImporting);
            logger.debug("Converted importing country: {} -> {}", originalImporting, convertedImporting);
        } else {
            logger.warn("Failed to convert importing country: {}", originalImporting);
        }

        if (convertedExporting != null) {
            data.setExportingCountry(convertedExporting);
            logger.debug("Converted exporting country: {} -> {}", originalExporting, convertedExporting);
        } else {
            logger.warn("Failed to convert exporting country: {}", originalExporting);
        }

        return data;
    }

    /**
     * Convert country names in scraped data to 2-digit ISO codes
     */
    private void convertCountryNamesToCodes(ScrapedTariffResponse response) {
        if (response.getData() == null) {
            return;
        }

        List<ScrapedTariffData> convertedData = response.getData().stream()
                .map(this::convertTariffDataCountryCodes)
                .collect(Collectors.toList());

        response.setData(convertedData);
        logger.debug("Converted country names to ISO2 codes for {} records", convertedData.size());
    }

    private ScrapedTariffResponse createErrorResponse(String importCode, String exportCode, String errorMessage) {
        ScrapedTariffResponse errorResponse = new ScrapedTariffResponse();
        errorResponse.setStatus("error");
        errorResponse.setSource_url(String.format("https://wits.worldbank.org/tariff/trains/en/country/%s/partner/%s/product/all", importCode, exportCode));
        errorResponse.setChapter("01");
        errorResponse.setResults_count(0);
        errorResponse.setData(Collections.emptyList());
        logger.debug("Created error response for {}->{}: {}", exportCode, importCode, errorMessage);
        return errorResponse;
    }

    // Health check method to verify Python API is running
    public boolean isScraperHealthy() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PYTHON_API_BASE_URL + "/health"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            logger.debug("Scraper health check response: {}", response.body());
            return response.statusCode() == 200 && response.body() != null && response.body().contains("healthy");
        } catch (Exception e) {
            logger.warn("Scraper health check failed: {}", e.getMessage());
            return false;
        }
    }
}
