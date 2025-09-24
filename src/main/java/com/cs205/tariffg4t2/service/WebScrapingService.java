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
import java.io.InputStream;
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
import java.util.zip.GZIPInputStream;

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
    private static final int MAX_CONTENT_LENGTH = 25000; // Limit content sent to LLM

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
     * Fetch web content from URL with enhanced handling for UK Trade Tariff
     */
    private String fetchWebContent(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Enhanced headers specifically for UK government sites
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        connection.setRequestProperty("Accept-Language", "en-GB,en;q=0.9,en-US;q=0.8");
        connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
        connection.setRequestProperty("DNT", "1");
        connection.setRequestProperty("Connection", "keep-alive");
        connection.setRequestProperty("Upgrade-Insecure-Requests", "1");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Pragma", "no-cache");
        // Add gov.uk specific headers
        connection.setRequestProperty("Referer", "https://www.trade-tariff.service.gov.uk/");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);

        int responseCode = connection.getResponseCode();
        logger.info("HTTP Response Code for {}: {}", urlString, responseCode);

        if (responseCode != 200) {
            throw new RuntimeException("HTTP error code: " + responseCode);
        }

        // Handle gzip encoding
        InputStream inputStream = connection.getInputStream();
        String encoding = connection.getContentEncoding();
        if ("gzip".equalsIgnoreCase(encoding)) {
            inputStream = new GZIPInputStream(inputStream);
        }

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
            String line;
            int totalLength = 0;
            int lineCount = 0;
            while ((line = reader.readLine()) != null && totalLength < MAX_CONTENT_LENGTH) {
                content.append(line).append("\n");
                totalLength += line.length();
                lineCount++;
            }
            logger.info("Fetched {} lines, {} characters from {}", lineCount, totalLength, urlString);
        }

        String fetchedContent = content.toString();

        // LOG FULL CONTENT FOR DEBUGGING
        logger.info("=== FULL CONTENT FROM {} ===", urlString);
        logger.info("FULL CONTENT:\n{}", fetchedContent);
        logger.info("=== END FULL CONTENT ===");

        // Check for specific UK tariff indicators
        if (fetchedContent.contains("commodity code") || fetchedContent.contains("tariff rate") ||
                fetchedContent.contains("duty rate") || fetchedContent.contains("third country")) {
            logger.info("Found UK tariff indicators in content");
        }

        // Check if content suggests JavaScript is needed
        if (fetchedContent.toLowerCase().contains("javascript") &&
                fetchedContent.toLowerCase().contains("enable") ||
                fetchedContent.contains("noscript")) {
            logger.warn("Page may require JavaScript to load content");
        }

        return fetchedContent;
    }

    /**
     * Debug method to analyze raw content
     */
    public void debugWebContent(String url) {
        try {
            String content = fetchWebContent(url);
            logger.info("=== CONTENT ANALYSIS FOR {} ===", url);
            logger.info("Total content length: {}", content.length());

            // Check for common tariff-related keywords
            String[] keywords = {"tariff", "duty", "rate", "%", "£", "€", "$", "code", "commodity", "import", "tax"};
            for (String keyword : keywords) {
                long count = content.toLowerCase().chars()
                        .mapToObj(c -> String.valueOf((char) c))
                        .mapToLong(s -> s.equals(keyword.toLowerCase()) ? 1 : 0)
                        .sum();
                if (content.toLowerCase().contains(keyword)) {
                    logger.info("Found keyword '{}': {} occurrences", keyword,
                            content.toLowerCase().split(keyword, -1).length - 1);
                }
            }

            // Check for numerical patterns
            Pattern numberPattern = Pattern.compile("\\d+\\.\\d+");
            Matcher matcher = numberPattern.matcher(content);
            int numberCount = 0;
            while (matcher.find() && numberCount < 10) {
                logger.info("Found number pattern: {}", matcher.group());
                numberCount++;
            }

            // Log content structure
            if (content.toLowerCase().contains("<table")) {
                logger.info("Content contains HTML tables");
            }
            if (content.toLowerCase().contains("<script")) {
                logger.info("Content contains JavaScript");
            }
            if (content.toLowerCase().contains("json")) {
                logger.info("Content mentions JSON");
            }

        } catch (Exception e) {
            logger.error("Failed to debug content for URL: {}", url, e);
        }
    }

    /**
     * Extract structured data using LLM
     */
    private String extractDataWithLLM(String webContent, String siteIdentifier) throws Exception {
        // Truncate content to stay within token limits (save ~2000 tokens for prompt and response)
        String truncatedContent = truncateContent(webContent, 2500);
        String prompt = buildExtractionPrompt(truncatedContent, siteIdentifier);

        // Create request payload for OpenAI API
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4");
        requestBody.put("max_tokens", 1000);
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
     * Build prompt for LLM extraction
     */
    private String buildExtractionPrompt(String webContent, String siteIdentifier) {
        StringBuilder prompt = new StringBuilder();

        if ("uk_trade_tariff".equals(siteIdentifier)) {
            prompt.append("You are analyzing content from the UK Government Trade Tariff website.\n\n");
            prompt.append("CRITICAL: This is a specific commodity subheading page. Look for:\n\n");

            prompt.append("SPECIFIC PATTERNS TO FIND:\n");
            prompt.append("1. Commodity codes starting with numbers (like 0201100000, 0201.10.00.00)\n");
            prompt.append("2. ANY percentage rates (like 12.8%, 0%, 20.0%)\n");
            prompt.append("3. Specific rate amounts (like £123 per tonne, €45 per 100kg)\n");
            prompt.append("4. Words: 'Third country duty', 'MFN', 'Tariff rate', 'Import duty'\n");
            prompt.append("5. Tables with duty information\n");
            prompt.append("6. Product descriptions (meat, beef, frozen, etc.)\n");
            prompt.append("7. Look in <td>, <th>, <div>, <span> tags\n");
            prompt.append("8. Look for JSON data embedded in <script> tags\n");
            prompt.append("9. Search for 'duty_expression', 'measure_type', 'geographical_area'\n\n");

            prompt.append("EXTRACTION RULES:\n");
            prompt.append("- If you find a 10-digit code like 0201100000, use it as hs_code\n");
            prompt.append("- If you find percentages, extract the number (12.8% becomes '12.8')\n");
            prompt.append("- Look for 'Third country duty' or 'Standard rate' sections\n");
            prompt.append("- Product descriptions often contain 'meat', 'beef', 'carcases', 'frozen'\n");
            prompt.append("- importing_country should be 'GB' or 'UK'\n\n");
        }

        prompt.append("Extract tariff information from this UK Trade Tariff webpage.\n\n");
        prompt.append("Return ONLY valid JSON in this exact format:\n");
        prompt.append("{\n");
        prompt.append("  \"tariff_data\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"hs_code\": \"0201100000\",\n");
        prompt.append("      \"product_description\": \"Meat of bovine animals, fresh or chilled, Carcases and half-carcases\",\n");
        prompt.append("      \"rate\": \"12.8\",\n");
        prompt.append("      \"unit\": \"ad valorem\",\n");
        prompt.append("      \"importing_country\": \"GB\",\n");
        prompt.append("      \"additional_info\": \"Third country duty rate\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        prompt.append("IMPORTANT REMINDERS:\n");
        prompt.append("- Scan the ENTIRE content, including HTML tags and JavaScript\n");
        prompt.append("- Look for numbers followed by % symbols\n");
        prompt.append("- Check for JSON objects in <script> tags\n");
        prompt.append("- If you see 'Free' or '0%', use rate: '0'\n");
        prompt.append("- If no data found, return: {\"tariff_data\": []}\n\n");
        prompt.append("Content to analyze:\n");
        prompt.append(webContent);

        return prompt.toString();
    }

    /**
     * Parse LLM response and save data to database
     */
    private int parseAndSaveData(String llmResponse, ScrapingJob scrapingJob) throws Exception {
        int recordsExtracted = 0;
        //log the LLMResponse
        logger.info("LLM response is: {}", llmResponse);
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
     * Truncate content to stay within OpenAI token limits
     */
    private String truncateContent(String content, int maxTokens) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        // Rough estimate: 1 token ≈ 4 characters
        // Leave some buffer for prompt overhead
        int maxChars = maxTokens * 3; // Conservative estimate

        if (content.length() <= maxChars) {
            return content;
        }

        // Truncate and add indicator
        String truncated = content.substring(0, maxChars);

        // Try to cut at a reasonable boundary (end of line, paragraph, etc.)
        int lastNewline = truncated.lastIndexOf('\n');
        int lastParagraph = truncated.lastIndexOf("\n\n");

        if (lastParagraph > maxChars * 0.8) {
            truncated = content.substring(0, lastParagraph);
        } else if (lastNewline > maxChars * 0.8) {
            truncated = content.substring(0, lastNewline);
        }

        return truncated + "\n\n[Content truncated to fit token limits...]";
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
     * Enhanced scrapeUrl method with better debugging
     */
    @Transactional
    public ScrapingJob scrapeUrlWithDebugging(TargetUrl targetUrl) {
        ScrapingJob scrapingJob = new ScrapingJob();
        scrapingJob.setTargetUrl(targetUrl);
        scrapingJob.setStartTime(LocalDateTime.now());
        scrapingJob.setStatus("IN_PROGRESS");

        scrapingJob = scrapingRepositoryJob.save(scrapingJob);

        try {
            logger.info("=== STARTING ENHANCED DEBUGGING FOR: {} ===", targetUrl.getUrl());

            // Step 1: Debug the raw content first
            debugWebContent(targetUrl.getUrl());

            // Step 2: Fetch content with enhanced logging
            String webContent = fetchWebContent(targetUrl.getUrl());
            logger.info("Fetched content length: {} characters", webContent.length());

            // Step 3: Test LLM connection first
            testLLMConnection();

            // Step 4: Use enhanced LLM extraction
            String extractedData = extractDataWithLLM(webContent, targetUrl.getSiteIdentifier());
            logger.info("LLM extraction result length: {} characters", extractedData.length());
            logger.info("Full LLM response: {}", extractedData);

            // Step 5: Parse and save
            int recordsExtracted = parseAndSaveData(extractedData, scrapingJob);

            scrapingJob.setEndTime(LocalDateTime.now());
            scrapingJob.setStatus("SUCCESS");
            scrapingJob.setRecordsExtracted(recordsExtracted);

            targetUrl.setLastScraped(LocalDateTime.now());
            targetUrlRepository.save(targetUrl);

            logger.info("=== DEBUGGING COMPLETE: {} records extracted ===", recordsExtracted);

        } catch (Exception e) {
            logger.error("Enhanced scraping failed for URL {}: {}", targetUrl.getUrl(), e.getMessage(), e);

            scrapingJob.setEndTime(LocalDateTime.now());
            scrapingJob.setStatus("FAILED");
            scrapingJob.setErrorMessage(e.getMessage());
        }

        return scrapingRepositoryJob.save(scrapingJob);
    }

    /**
     * Enhanced method to show exactly what the LLM receives
     */
    @Transactional
    public ScrapingJob scrapeUrlWithFullLogging(TargetUrl targetUrl) {
        ScrapingJob scrapingJob = new ScrapingJob();
        scrapingJob.setTargetUrl(targetUrl);
        scrapingJob.setStartTime(LocalDateTime.now());
        scrapingJob.setStatus("IN_PROGRESS");

        scrapingJob = scrapingRepositoryJob.save(scrapingJob);

        try {
            logger.info("=== STARTING FULL CONTENT LOGGING FOR: {} ===", targetUrl.getUrl());

            // Step 1: Fetch content
            String webContent = fetchWebContent(targetUrl.getUrl());
            logger.info("Raw content length: {} characters", webContent.length());

            // Step 2: Build the exact prompt that will be sent to LLM
            String prompt = buildExtractionPrompt(webContent, targetUrl.getSiteIdentifier());
            logger.info("=== EXACT PROMPT BEING SENT TO LLM ===");
            logger.info("PROMPT:\n{}", prompt);
            logger.info("=== END LLM PROMPT ===");

            // Step 3: Test LLM connection
            String testResult = testLLMConnection();
            logger.info("LLM test result: {}", testResult);

            // Step 4: Send to LLM
            String extractedData = extractDataWithLLM(webContent, targetUrl.getSiteIdentifier());
            logger.info("=== RAW LLM RESPONSE ===");
            logger.info("LLM RESPONSE:\n{}", extractedData);
            logger.info("=== END LLM RESPONSE ===");

            // Step 5: Parse response
            int recordsExtracted = parseAndSaveData(extractedData, scrapingJob);

            scrapingJob.setEndTime(LocalDateTime.now());
            scrapingJob.setStatus("SUCCESS");
            scrapingJob.setRecordsExtracted(recordsExtracted);

            targetUrl.setLastScraped(LocalDateTime.now());
            targetUrlRepository.save(targetUrl);

            logger.info("=== FULL LOGGING COMPLETE: {} records extracted ===", recordsExtracted);

        } catch (Exception e) {
            logger.error("Full logging scraping failed for URL {}: {}", targetUrl.getUrl(), e.getMessage(), e);

            scrapingJob.setEndTime(LocalDateTime.now());
            scrapingJob.setStatus("FAILED");
            scrapingJob.setErrorMessage(e.getMessage());
        }

        return scrapingRepositoryJob.save(scrapingJob);
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
    public String testLLMConnection() {
        try {
            String testPrompt = "Return exactly this JSON: {\"test\": \"success\"}";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4");
            requestBody.put("max_tokens", 100);
            requestBody.put("temperature", 0.1);

            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", testPrompt);
            requestBody.put("messages", List.of(message));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(openAiApiUrl, entity, String.class);

            logger.info("LLM test response: {}", response.getBody());
            return response.getBody();

        } catch (Exception e) {
            logger.error("LLM test failed", e);
            return "LLM test failed: " + e.getMessage();
        }
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
     * Enhanced debugging specifically for UK Trade Tariff URLs
     */
    public void debugUKTariffContent(String url) {
        try {
            String content = fetchWebContent(url);
            logger.info("=== UK TARIFF CONTENT ANALYSIS FOR {} ===", url);
            logger.info("Total content length: {}", content.length());

            // Check for UK-specific tariff patterns
            String[] ukPatterns = {
                    "commodity code", "tariff rate", "third country", "duty rate", "MFN",
                    "measure_type", "duty_expression", "geographical_area", "0201100000",
                    "12.8%", "ad valorem", "bovine", "meat", "carcases"
            };

            for (String pattern : ukPatterns) {
                if (content.toLowerCase().contains(pattern.toLowerCase())) {
                    int count = content.toLowerCase().split(Pattern.quote(pattern.toLowerCase()), -1).length - 1;
                    logger.info("Found UK pattern '{}': {} occurrences", pattern, count);
                }
            }

            // Look for JSON data in script tags
            Pattern jsonPattern = Pattern.compile("<script[^>]*>.*?\"duty_expression\".*?</script>", Pattern.DOTALL);
            Matcher jsonMatcher = jsonPattern.matcher(content);
            if (jsonMatcher.find()) {
                logger.info("Found JSON tariff data in script tags");
                logger.info("JSON sample: {}", jsonMatcher.group().substring(0, Math.min(500, jsonMatcher.group().length())));
            }

            // Look for table structures
            Pattern tablePattern = Pattern.compile("<table[^>]*>.*?</table>", Pattern.DOTALL);
            Matcher tableMatcher = tablePattern.matcher(content);
            int tableCount = 0;
            while (tableMatcher.find() && tableCount < 3) {
                logger.info("Found table {}: {}", tableCount + 1,
                        tableMatcher.group().substring(0, Math.min(300, tableMatcher.group().length())));
                tableCount++;
            }

        } catch (Exception e) {
            logger.error("Failed to debug UK tariff content for URL: {}", url, e);
        }
    }

    /**
     * Enhanced scraping method specifically for UK Trade Tariff
     */
    @Transactional
    public ScrapingJob scrapeUKTariffUrl(String urlString) {
        try {
            // Create or find target URL
            TargetUrl targetUrl = targetUrlRepository.findByUrl(urlString)
                    .orElseGet(() -> {
                        TargetUrl newUrl = new TargetUrl();
                        newUrl.setUrl(urlString);
                        newUrl.setSiteIdentifier("uk_trade_tariff");
                        newUrl.setScrapeFrequency("DAILY");
                        newUrl.setActive(true);
                        return targetUrlRepository.save(newUrl);
                    });

            logger.info("=== STARTING UK TARIFF SCRAPING FOR: {} ===", urlString);

            // Step 1: Debug content first
            debugUKTariffContent(urlString);

            // Step 2: Use enhanced debugging scraper
            return scrapeUrlWithDebugging(targetUrl);

        } catch (Exception e) {
            logger.error("Failed to scrape UK tariff URL: {}", urlString, e);
            throw new RuntimeException("UK tariff scraping failed", e);
        }
    }

    /**
     * Enhanced UK tariff scraping with full content logging
     */
    @Transactional
    public ScrapingJob scrapeUKTariffUrlWithFullLogging(String urlString) {
        try {
            // Create or find target URL
            TargetUrl targetUrl = targetUrlRepository.findByUrl(urlString)
                    .orElseGet(() -> {
                        TargetUrl newUrl = new TargetUrl();
                        newUrl.setUrl(urlString);
                        newUrl.setSiteIdentifier("uk_trade_tariff");
                        newUrl.setScrapeFrequency("DAILY");
                        newUrl.setActive(true);
                        return targetUrlRepository.save(newUrl);
                    });

            logger.info("=== STARTING UK TARIFF SCRAPING WITH FULL LOGGING FOR: {} ===", urlString);

            // Use full logging version
            return scrapeUrlWithFullLogging(targetUrl);

        } catch (Exception e) {
            logger.error("Failed to scrape UK tariff URL with full logging: {}", urlString, e);
            throw new RuntimeException("UK tariff scraping with full logging failed", e);
        }
    }
}

