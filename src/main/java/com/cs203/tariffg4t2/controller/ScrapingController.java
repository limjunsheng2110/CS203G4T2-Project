package com.cs203.tariffg4t2.controller;

import com.cs203.tariffg4t2.dto.scraping.ScrapedTariffResponse;
import com.cs203.tariffg4t2.service.data.WebScrapingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/scraping")
@CrossOrigin(origins = "*")
public class ScrapingController {

    private static final Logger logger = LoggerFactory.getLogger(ScrapingController.class);

    @Autowired
    private WebScrapingService webScrapingService;

    @PostMapping("/tariff")
    public ResponseEntity<?> scrapeTariffData(
            @RequestParam String importCode,
            @RequestParam String exportCode) {

        logger.info("Received scraping request for import: {}, export: {}", importCode, exportCode);

        // Validate input parameters
        if (importCode == null || importCode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("Import code is required"));
        }

        if (exportCode == null || exportCode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("Export code is required"));
        }

        // Trim and uppercase the codes for further processing
        String trimmedImportCode = importCode.trim().toUpperCase();
        String trimmedExportCode = exportCode.trim().toUpperCase();

        // Validate country code format (should be 2-3 character codes)
        if (trimmedImportCode.length() < 2 || trimmedImportCode.length() > 3) {
            return ResponseEntity.badRequest().body(createErrorResponse("Import code must be 2-3 characters"));
        }

        if (trimmedExportCode.length() < 2 || trimmedExportCode.length() > 3) {
            return ResponseEntity.badRequest().body(createErrorResponse("Export code must be 2-3 characters"));
        }

        try {
            // Call the web scraping service
            ScrapedTariffResponse response = webScrapingService.scrapeTariffData(
                    trimmedImportCode,
                    trimmedExportCode
            );

            if ("error".equals(response.getStatus())) {
                logger.warn("Scraping failed for {}->{}", exportCode, importCode);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            logger.info("Successfully completed scraping for {}->{} with {} results",
                    exportCode, importCode, response.getResults_count());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Unexpected error during scraping for {}->{}: {}", exportCode, importCode, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> checkScraperHealth() {
        logger.debug("Checking scraper health");

        try {
            boolean isHealthy = webScrapingService.isScraperHealthy();

            Map<String, Object> healthResponse = new HashMap<>();
            healthResponse.put("scraper_status", isHealthy ? "healthy" : "unhealthy");
            healthResponse.put("timestamp", System.currentTimeMillis());

            if (isHealthy) {
                return ResponseEntity.ok(healthResponse);
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(healthResponse);
            }

        } catch (Exception e) {
            logger.error("Error checking scraper health: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Health check failed: " + e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getScrapingStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "Tariff Scraping Service");
        status.put("version", "1.0");
        status.put("chapter_focus", "Chapter 1 - Live Animals");
        status.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(status);
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        return errorResponse;
    }
}
