package com.cs205.tariffg4t2.service.data;

import com.cs205.tariffg4t2.dto.basic.TariffDataDTO;
import com.cs205.tariffg4t2.model.basic.*;
import com.cs205.tariffg4t2.model.web.*;
import com.cs205.tariffg4t2.repository.*;
import com.cs205.tariffg4t2.repository.basic.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WebScrapingService {

    private static final Logger logger = LoggerFactory.getLogger(WebScrapingService.class);

    @Autowired
    private TargetUrlRepository targetUrlRepository;

    @Autowired
    private ScrapingRepositoryJob scrapingRepositoryJob;

    @Autowired
    private TariffRateDetailRepository tariffRateDetailRepository;

    @Autowired
    private TariffRateRepository tariffRateRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Value("${python.scraper.url}")
    private String pythonScraperUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Configuration
    private static final int REQUEST_DELAY_MS = 3000; // 3 seconds between requests

    /**
     * Scrape all URLs that are due for scraping
     */
    public void scrapeAllDueUrls() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayAgo = now.minusDays(1);
        LocalDateTime weekAgo = now.minusWeeks(1);
        LocalDateTime monthAgo = now.minusMonths(1);

        List<TargetUrl> dueUrls = targetUrlRepository.findUrlsDueForScraping(dayAgo, weekAgo, monthAgo);

        logger.info("Found {} URLs due for scraping", dueUrls.size());

        for (TargetUrl targetUrl : dueUrls) {
            try {
                scrapeUrl(targetUrl);
                // Add delay between requests to be respectful
                Thread.sleep(REQUEST_DELAY_MS);
            } catch (Exception e) {
                logger.error("Error scraping URL {}: {}", targetUrl.getUrl(), e.getMessage());
            }
        }
    }

    /**
     * Scrape a specific URL using Python microservice
     */
    @Transactional
    public ScrapingJob scrapeUrl(TargetUrl targetUrl) {
        ScrapingJob scrapingJob = new ScrapingJob();
        scrapingJob.setTargetUrl(targetUrl);
        scrapingJob.setStartTime(LocalDateTime.now());
        scrapingJob.setStatus("IN_PROGRESS");

        // Save the job immediately
        scrapingJob = scrapingRepositoryJob.save(scrapingJob);

        try {
            logger.info("Starting Python microservice scraping for URL: {}", targetUrl.getUrl());

            // Call Python microservice with the target URL
            String serviceUrl = UriComponentsBuilder.fromHttpUrl(pythonScraperUrl)
                    .queryParam("target_url", targetUrl.getUrl())
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "TariffBot/1.0 Spring-Boot-Service");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);

            logger.info("Calling Python service at: {}", serviceUrl);

            ResponseEntity<String> response = restTemplate.exchange(
                    serviceUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Python service responded successfully");
                logger.info("Raw response: {}", response.getBody());

                // Parse the response from Python microservice
                List<TariffDataDTO> tariffDataList = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<TariffDataDTO>>() {}
                );

                logger.info("Parsed {} tariff data items from Python service", tariffDataList.size());

                // Process the tariff data
                int recordsExtracted = processTariffData(tariffDataList, scrapingJob);

                // Update job as successful
                scrapingJob.setEndTime(LocalDateTime.now());
                scrapingJob.setStatus("SUCCESS");
                scrapingJob.setRecordsExtracted(recordsExtracted);

                // Update target URL last scraped time
                targetUrl.setLastScraped(LocalDateTime.now());
                targetUrlRepository.save(targetUrl);

                logger.info("Successfully extracted {} records using Python microservice for {}",
                        recordsExtracted, targetUrl.getUrl());

            } else {
                throw new RuntimeException("Python microservice call failed with status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("Python microservice scraping failed for URL {}: {}", targetUrl.getUrl(), e.getMessage(), e);

            // Mark job as failed
            scrapingJob.setEndTime(LocalDateTime.now());
            scrapingJob.setStatus("FAILED");
            // Truncate error message to avoid database issues
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500) + "...";
            }
            scrapingJob.setErrorMessage(errorMsg);
        }

        return scrapingRepositoryJob.save(scrapingJob);
    }

    /**
     * Process tariff data from Python microservice
     */

    private int processTariffData(List<TariffDataDTO> tariffDataList, ScrapingJob scrapingJob) {
        int recordsProcessed = 0;

        for (TariffDataDTO tariffData : tariffDataList) {
            try {
                logger.info("Processing tariff data: {}", tariffData);

                // Extract and clean tariff rate
                String rateStr = tariffData.getTariffRate();
                if (rateStr == null || rateStr.trim().isEmpty()) {
                    logger.warn("Empty tariff rate for item: {}", tariffData);
                    continue;
                }

                // Clean the rate (remove %, spaces, etc.)
                String cleanRate = rateStr.replaceAll("[^0-9.]", "");
                if (cleanRate.isEmpty()) {
                    logger.warn("Could not extract numeric rate from: {}", rateStr);
                    continue;
                }

                BigDecimal rate = new BigDecimal(cleanRate);

                // Map countries
                String importingCountry = mapCountryName(tariffData.getImportedFrom());
                String exportingCountry = mapCountryName(tariffData.getExportedFrom());

                // Generate UNIQUE HS code from product type
                String hsCode = generateUniqueHsCodeFromType(tariffData.getType(), recordsProcessed);

                // Create or find product (wrapped in try-catch)
                Product product = findOrCreateProductSafely(hsCode, tariffData.getType());
                if (product == null) {
                    logger.warn("Could not create/find product for HS code: {}", hsCode);
                    continue;
                }

                // Rest of your processing...
                recordsProcessed++;

            } catch (Exception e) {
                logger.warn("Failed to process tariff data item: {}", tariffData, e);
                // Continue processing other items even if one fails
            }
        }

        return recordsProcessed;
    }

    // Add this method to generate unique HS codes
    private String generateUniqueHsCodeFromType(String type, int index) {
        if (type == null) return String.format("0000%06d", index);

        String baseCode = generateHsCodeFromType(type);
        // Add index to make it unique
        return baseCode.substring(0, 6) + String.format("%04d", index);
    }

    // Add this safe method for product creation
    private Product findOrCreateProductSafely(String hsCode, String description) {
        try {
            return findOrCreateProduct(hsCode, description);
        } catch (Exception e) {
            logger.error("Failed to create/find product with HS code {}: {}", hsCode, e.getMessage());
            return null;
        }
    }



    /**
     * Generate HS code from product type (basic implementation)
     */
    private String generateHsCodeFromType(String type) {
        if (type == null) return "0000000000";

        // Basic mapping - you may want to enhance this with a proper lookup table
        String lowerType = type.toLowerCase();
        if (lowerType.contains("livestock") || lowerType.contains("beef") || lowerType.contains("meat")) {
            return "0201000000";
        } else if (lowerType.contains("electronics") || lowerType.contains("mobile") || lowerType.contains("phone")) {
            return "8517000000";
        } else if (lowerType.contains("textile") || lowerType.contains("clothing")) {
            return "6100000000";
        } else {
            // Generate a hash-based HS code for consistency
            int hash = Math.abs(type.hashCode());
            return String.format("%010d", hash % 10000000000L);
        }
    }

    /**
     * Map country names to codes
     */
    private String mapCountryName(String countryName) {
        if (countryName == null || countryName.trim().isEmpty()) {
            return "UNKNOWN";
        }

        String normalized = countryName.trim().toUpperCase();
        switch (normalized) {
            case "UNITED STATES":
            case "USA":
            case "US":
                return "USA";
            case "UNITED KINGDOM":
            case "UK":
            case "BRITAIN":
            case "GREAT BRITAIN":
                return "GBR";
            case "CHINA":
                return "CHN";
            case "SINGAPORE":
                return "SGP";
            case "GERMANY":
                return "DEU";
            case "FRANCE":
                return "FRA";
            case "JAPAN":
                return "JPN";
            default:
                return normalized;
        }
    }


    //make changes here.

    /**
     * Find or create Country entity
     */
    private Country findOrCreateCountry(String countryCode, String countryName) {
        Optional<Country> existing = countryRepository.findByCountryCode(countryCode);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new country
        Country country = new Country();
        country.setCountryCode(countryCode);
        country.setCountryName(countryName);
        return countryRepository.save(country);
    }

    /**
     * Find or create Product entity
     */
    private Product findOrCreateProduct(String hsCode, String description) {
        Optional<Product> existing = productRepository.findByHsCode(hsCode);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new product
        Product product = new Product();
        product.setHsCode(hsCode);
        product.setDescription(description != null ? description : "Unknown Product");
        product.setCategory("AI_Extracted"); // Mark as AI-extracted
        return productRepository.save(product);
    }

    /**
     * Save tariff data to database
     */
    private void saveTariffData(String hsCode, String importingCountry, String exportingCountry,
                                BigDecimal rate, String unit, ScrapingJob scrapingJob,
                                String productDesc, String additionalInfo) {

        // Find or create TariffRate
        Optional<TariffRate> existingTariffRate = tariffRateRepository
                .findByHsCodeAndImportingCountryCodeAndExportingCountryCode(hsCode, importingCountry, exportingCountry);

        TariffRate tariffRate;
        if (existingTariffRate.isPresent()) {
            tariffRate = existingTariffRate.get();
            // Update the base rate with new information
            tariffRate.setBaseRate(rate);
            tariffRate.setUnit(unit);
            tariffRate = tariffRateRepository.save(tariffRate);
        } else {
            tariffRate = new TariffRate();
            tariffRate.setHsCode(hsCode);
            tariffRate.setImportingCountryCode(importingCountry);
            tariffRate.setExportingCountryCode(exportingCountry);
            tariffRate.setBaseRate(rate);
            tariffRate.setUnit(unit);
            tariffRate = tariffRateRepository.save(tariffRate);
        }

        // Create TariffRateDetail
        TariffRateDetail detail = new TariffRateDetail();
        detail.setTariffRate(tariffRate);
        detail.setFinalRate(rate);
        detail.setDataSource("PYTHON_AI_SCRAPER");
        detail.setScraping(scrapingJob);
        detail.setConfidenceScore(new BigDecimal("0.85")); // High confidence for AI extraction
        detail.setEffectiveDate(LocalDateTime.now());

        // Add AI-extracted additional information
        String notes = "";
        if (productDesc != null && !productDesc.isEmpty()) {
            notes += "Product: " + productDesc;
        }
        if (additionalInfo != null && !additionalInfo.isEmpty()) {
            if (!notes.isEmpty()) notes += "; ";
            notes += additionalInfo;
        }
        detail.setNotes(notes);

        tariffRateDetailRepository.save(detail);
        logger.info("Saved tariff rate detail for HS code: {} with rate: {}%", hsCode, rate);
    }

    /**
     * Get tariff rate for calculation purposes (existing method)
     */
    public BigDecimal getTariffRate(String homeCountry, String destinationCountry,
                                    String hsCode, String productCategory) {

        Optional<TariffRate> existingRate = tariffRateRepository
                .findByHsCodeAndImportingCountryCodeAndExportingCountryCode(hsCode, destinationCountry, homeCountry);

        if (existingRate.isPresent()) {
            // Get the most recent detail for this rate
            Optional<TariffRateDetail> latestDetail = tariffRateDetailRepository
                    .findFirstByTariffRateAndIsActiveTrueOrderByCreatedAtDesc(existingRate.get());

            if (latestDetail.isPresent()) {
                return latestDetail.get().getFinalRate();
            } else {
                return existingRate.get().getBaseRate();
            }
        }

        // If not found, return a default rate
        logger.warn("No tariff rate found for HS code {} from {} to {}", hsCode, homeCountry, destinationCountry);
        return new BigDecimal("5.0"); // Default 5% tariff for testing
    }

    /**
     * Get scraping statistics
     */
    public Object getScrapingStats() {
        long totalJobs = scrapingRepositoryJob.count();
        long successfulJobs = scrapingRepositoryJob.findByStatusOrderByStartTimeDesc("SUCCESS").size();
        long failedJobs = scrapingRepositoryJob.findByStatusOrderByStartTimeDesc("FAILED").size();

        return new Object() {
            public final long total = totalJobs;
            public final long successful = successfulJobs;
            public final long failed = failedJobs;
            public final double successRate = totalJobs > 0 ? (successfulJobs * 100.0 / totalJobs) : 0;
        };
    }

    /**
     * Test method to verify Python microservice connectivity
     */
    public String testPythonMicroservice() {
        try {
            String testUrl = "https://example.com";
            String serviceUrl = UriComponentsBuilder.fromHttpUrl(pythonScraperUrl)
                    .queryParam("target_url", testUrl)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    serviceUrl, HttpMethod.GET, entity, String.class);

            return "Python microservice test successful. Status: " + response.getStatusCode() +
                    ", Response length: " + (response.getBody() != null ? response.getBody().length() : 0);

        } catch (Exception e) {
            return "Python microservice test failed: " + e.getMessage();
        }
    }
}