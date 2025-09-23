package com.cs205.tariffg4t2.service;

import com.cs205.tariffg4t2.model.api.*;
import com.cs205.tariffg4t2.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
    
    @Value("${openai.api.key}")
    private String openAiApiKey;
    
    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openAiApiUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Configuration
    private static final int REQUEST_DELAY_MS = 3000; // 3 seconds between requests
    private static final int MAX_CONTENT_LENGTH = 50000; // Limit content sent to LLM
    
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
     * Scrape a specific URL using LLM
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
            logger.info("Starting LLM-powered scraping for URL: {}", targetUrl.getUrl());
            
            // Step 1: Fetch the web page content
            String webContent = fetchWebContent(targetUrl.getUrl());
            
            // Step 2: Use LLM to extract structured data
            String extractedData = extractDataWithLLM(webContent, targetUrl.getSiteIdentifier());
            
            // Step 3: Parse and save the extracted data
            int recordsExtracted = parseAndSaveData(extractedData, scrapingJob);
            
            // Update job as successful
            scrapingJob.setEndTime(LocalDateTime.now());
            scrapingJob.setStatus("SUCCESS");
            scrapingJob.setRecordsExtracted(recordsExtracted);
            
            // Update target URL last scraped time
            targetUrl.setLastScraped(LocalDateTime.now());
            targetUrlRepository.save(targetUrl);
            
            logger.info("Successfully scraped {} records from {}", recordsExtracted, targetUrl.getUrl());
            
        } catch (Exception e) {
            logger.error("Scraping failed for URL {}: {}", targetUrl.getUrl(), e.getMessage(), e);
            
            // Mark job as failed
            scrapingJob.setEndTime(LocalDateTime.now());
            scrapingJob.setStatus("FAILED");
            scrapingJob.setErrorMessage(e.getMessage());
        }
        
        return scrapingRepositoryJob.save(scrapingJob);
    }
    
    /**
     * Fetch web content from URL
     */
    private String fetchWebContent(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // Set headers to mimic a real browser
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", 
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("HTTP error code: " + responseCode);
        }
        
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            int totalLength = 0;
            while ((line = reader.readLine()) != null && totalLength < MAX_CONTENT_LENGTH) {
                content.append(line).append("\n");
                totalLength += line.length();
            }
        }
        
        return content.toString();
    }
    
    /**
     * Extract structured data using LLM
     */
    private String extractDataWithLLM(String webContent, String siteIdentifier) throws Exception {
        // Prepare the prompt for the LLM
        String prompt = buildExtractionPrompt(webContent, siteIdentifier);
        
        // Create request payload for OpenAI API
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4");
        requestBody.put("max_tokens", 2000);
        requestBody.put("temperature", 0.1); // Low temperature for consistent extraction
        
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        requestBody.put("messages", List.of(message));
        
        // Set up headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);
        
        // Make the API call
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(openAiApiUrl, entity, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            // Parse the response to get the extracted data
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            return jsonResponse.path("choices").get(0).path("message").path("content").asText();
        } else {
            throw new RuntimeException("LLM API call failed with status: " + response.getStatusCode());
        }
    }
    
    /**
     * Build extraction prompt for the LLM
     */
    private String buildExtractionPrompt(String webContent, String siteIdentifier) {
        StringBuilder prompt = new StringBuilder();

        // specifically for UK case
        if ("uk_trade_tariff".equals(siteIdentifier)) {
            prompt.append("This is from the UK Government Trade Tariff (gov.uk/trade-tariff).\n");
            prompt.append("Look specifically for:\n");
            prompt.append("- Commodity codes (10-digit UK tariff codes or HS codes)\n");
            prompt.append("- Duty rates (percentages or specific amounts)\n");
            prompt.append("- Product descriptions\n");
            prompt.append("- Third country duty rates\n");
            prompt.append("- Preferential rates for specific countries\n");
            prompt.append("- Units of measurement\n\n");
        }
        
        prompt.append("You are a data extraction expert. Extract tariff rate information from the following webpage content.\n\n");
        prompt.append("Target website: ").append(siteIdentifier).append("\n\n");
        prompt.append("Instructions:\n");
        prompt.append("1. Look for tariff rates, customs duties, import taxes, or similar information\n");
        prompt.append("2. Extract HS codes, product descriptions, tax rates, and units\n");
        prompt.append("3. Return the data in JSON format with this structure:\n");
        prompt.append("{\n");
        prompt.append("  \"tariff_data\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"hs_code\": \"0101.21\",\n");
        prompt.append("      \"product_description\": \"Live horses - Pure-bred breeding animals\",\n");
        prompt.append("      \"rate\": \"5.0\",\n");
        prompt.append("      \"unit\": \"ad valorem\",\n");
        prompt.append("      \"importing_country\": \"SG\",\n");
        prompt.append("      \"additional_info\": \"any relevant notes\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        prompt.append("4. If no tariff data is found, return: {\"tariff_data\": []}\n");
        prompt.append("5. Only include data you are confident about\n\n");
        prompt.append("Webpage content to analyze:\n\n");
        prompt.append(webContent);
        
        return prompt.toString();
    }
    
    /**
     * Parse LLM response and save data to database
     */
    private int parseAndSaveData(String llmResponse, ScrapingJob scrapingJob) throws Exception {
        int recordsExtracted = 0;
        
        try {
            // Clean up the response (remove any extra text before/after JSON)
            String cleanJson = extractJsonFromResponse(llmResponse);
            
            JsonNode jsonNode = objectMapper.readTree(cleanJson);
            JsonNode tariffData = jsonNode.path("tariff_data");
            
            if (tariffData.isArray()) {
                for (JsonNode item : tariffData) {
                    try {
                        String hsCode = item.path("hs_code").asText();
                        String rate = item.path("rate").asText();
                        String unit = item.path("unit").asText("ad valorem");
                        String importingCountry = item.path("importing_country").asText("UNKNOWN");
                        String productDesc = item.path("product_description").asText();
                        String additionalInfo = item.path("additional_info").asText();
                        
                        if (!hsCode.isEmpty() && !rate.isEmpty()) {
                            BigDecimal rateValue = new BigDecimal(rate.replaceAll("[^0-9.]", ""));
                            
                            saveTariffData(hsCode, importingCountry, "UNKNOWN", rateValue, unit, scrapingJob, productDesc, additionalInfo);
                            recordsExtracted++;
                        }
                    } catch (Exception e) {
                        logger.warn("Could not parse tariff item: {}", item.toString(), e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to parse LLM response: {}", llmResponse, e);
            throw e;
        }
        
        return recordsExtracted;
    }
    
    /**
     * Extract JSON from LLM response (handles cases where LLM adds explanation text)
     */
    private String extractJsonFromResponse(String response) {
        // Look for JSON pattern
        Pattern jsonPattern = Pattern.compile("\\{[\\s\\S]*\\}", Pattern.MULTILINE);
        Matcher matcher = jsonPattern.matcher(response);
        
        if (matcher.find()) {
            return matcher.group();
        }
        
        // If no JSON found, assume the entire response is JSON
        return response;
    }
    
    /**
     * Save or update tariff data with additional information
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
        detail.setDataSource(scrapingJob.getTargetUrl().getSiteIdentifier());
        detail.setScraping(scrapingJob);
        detail.setConfidenceScore(new BigDecimal("0.90")); // Higher confidence with LLM extraction
        detail.setEffectiveDate(LocalDateTime.now());
        
        // Add LLM-extracted additional information
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
    }
    
    /**
     * Get tariff rate for calculation purposes
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
     * Test LLM extraction with sample content
     */
    public String testLLMExtraction(String sampleContent, String siteIdentifier) throws Exception {
        return extractDataWithLLM(sampleContent, siteIdentifier);
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
}