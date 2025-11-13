package com.cs203.tariffg4t2.service.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ConvertCodeService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, String> codeCache; // Cache for 2-digit to 3-digit conversions

    private static final String COUNTRY_API_URL = "https://restcountries.com/v3.1/alpha/";

    public ConvertCodeService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.codeCache = new ConcurrentHashMap<>();
        initializeCommonCodes();
    }

    /**
     * Convert 2-digit ISO country code to 3-digit ISO country code
     *
     * @param twoDigitCode The 2-digit ISO country code (e.g., "US", "SG", "MY")
     * @return The 3-digit ISO country code (e.g., "USA", "SGP", "MYS")
     */
    public String convertToISO3(String twoDigitCode) {
        if (twoDigitCode == null || twoDigitCode.trim().isEmpty()) {
            log.warn("Empty or null country code provided");
            return twoDigitCode;
        }

        String normalizedCode = twoDigitCode.trim().toUpperCase();

        // If it's already 3 digits, return as is
        if (normalizedCode.length() == 3) {
            log.debug("Code {} is already 3-digit format", normalizedCode);
            return normalizedCode;
        }

        // If it's not 2 digits, return as is
        if (normalizedCode.length() != 2) {
            log.warn("Invalid country code format: {}. Expected 2 or 3 characters", normalizedCode);
            return twoDigitCode;
        }

        // Check cache first
        if (codeCache.containsKey(normalizedCode)) {
            String cachedCode = codeCache.get(normalizedCode);
            log.debug("Found cached conversion: {} -> {}", normalizedCode, cachedCode);
            return cachedCode;
        }

        try {
            // Call REST Countries API to get 3-digit code
            String apiUrl = COUNTRY_API_URL + normalizedCode + "?fields=cca3";
            log.debug("Calling API: {}", apiUrl);

            String response = restTemplate.getForObject(apiUrl, String.class);

            if (response != null) {
                JsonNode jsonNode = objectMapper.readTree(response);
                String threeDigitCode = jsonNode.get("cca3").asText();

                // Cache the result
                codeCache.put(normalizedCode, threeDigitCode);
                log.info("Successfully converted {} to {}", normalizedCode, threeDigitCode);
                return threeDigitCode;
            }

        } catch (RestClientException e) {
            log.error("API call failed for country code {}: {}", normalizedCode, e.getMessage());
        } catch (Exception e) {
            log.error("Error parsing API response for country code {}: {}", normalizedCode, e.getMessage());
        }

        // If API fails, return the original code
        log.warn("Could not convert country code {}. Returning original code.", normalizedCode);
        return twoDigitCode;
    }

    /**
     * Initialize cache with common country code mappings to reduce API calls
     */
    private void initializeCommonCodes() {
        Map<String, String> commonMappings = new HashMap<>();

        // Common country mappings
        commonMappings.put("US", "USA");
        commonMappings.put("SG", "SGP");
        commonMappings.put("MY", "MYS");
        commonMappings.put("CN", "CHN");
        commonMappings.put("JP", "JPN");
        commonMappings.put("KR", "KOR");
        commonMappings.put("TH", "THA");
        commonMappings.put("VN", "VNM");
        commonMappings.put("ID", "IDN");
        commonMappings.put("PH", "PHL");
        commonMappings.put("IN", "IND");
        commonMappings.put("AU", "AUS");
        commonMappings.put("NZ", "NZL");
        commonMappings.put("GB", "GBR");
        commonMappings.put("DE", "DEU");
        commonMappings.put("FR", "FRA");
        commonMappings.put("IT", "ITA");
        commonMappings.put("ES", "ESP");
        commonMappings.put("NL", "NLD");
        commonMappings.put("BE", "BEL");
        commonMappings.put("CH", "CHE");
        commonMappings.put("CA", "CAN");
        commonMappings.put("MX", "MEX");
        commonMappings.put("BR", "BRA");
        commonMappings.put("AR", "ARG");
        commonMappings.put("CL", "CHL");

        codeCache.putAll(commonMappings);
        log.info("Initialized cache with {} common country code mappings", commonMappings.size());
    }

    /**
     * Clear the conversion cache
     */
    public void clearCache() {
        codeCache.clear();
        initializeCommonCodes(); // Re-initialize with common codes
        log.info("Cache cleared and re-initialized");
    }

    /**
     * Get cache size for monitoring
     */
    public int getCacheSize() {
        return codeCache.size();
    }

    /**
     * Check if a code conversion is cached
     */
    public boolean isCached(String twoDigitCode) {
        return codeCache.containsKey(twoDigitCode.toUpperCase());
    }
}
