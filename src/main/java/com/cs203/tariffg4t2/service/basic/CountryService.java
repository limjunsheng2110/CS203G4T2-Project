package com.cs203.tariffg4t2.service.basic;


import com.cs203.tariffg4t2.model.basic.Country;
import com.cs203.tariffg4t2.model.web.CountryAPI;
import com.cs203.tariffg4t2.repository.basic.CountryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

@Slf4j
@Service
public class CountryService {

    private final RestTemplate restTemplate;
    private static final String COUNTRIES_API_URL = "https://restcountries.com/v3.1/all?fields=cca2,cca3,name,region,currencies";
    private final Map<String, String> countryNameToIso3Cache = new HashMap<>();
    private final Map<String, String> countryNameToIso2Cache = new HashMap<>();

    @Autowired
    private CountryRepository countryRepository;

    public CountryService() {
        this.restTemplate = new RestTemplate();
        initializeCountryNameCache();
        initializeCountryNameToIso2Cache();
    }


    //API get all countries
    public List<Country> getAllCountries() {
        try {
            CountryAPI[] apiResponses = restTemplate.getForObject(
                    COUNTRIES_API_URL,
                    CountryAPI[].class
            );

            if (apiResponses == null) {
                throw new RuntimeException("No countries data received from API");
            }

            return Arrays.stream(apiResponses)
                    .filter(this::isValidCountry) // Filter out invalid entries
                    .map(this::mapToCountry)
                    .collect(Collectors.toList());

        } catch (RestClientException e) {
            throw new RuntimeException("Failed to fetch countries from API", e);
        }
    }

    public List<Country> getAllCountriesFromDatabase() {
        return countryRepository.findAll();
    }


    @Transactional
    public String populateCountriesDatabaseBatch() {
        // Clear existing data first (optional)
        countryRepository.deleteAll();

        // Get countries from API
        List<Country> countriesFromApi = getAllCountries();

        // Save all at once - let Spring handle the batching
        List<Country> savedCountries = countryRepository.saveAll(countriesFromApi);

        return "Successfully populated database with " + savedCountries.size() + " countries using batch method";
    }

    public long getCountriesCount() {
        return countryRepository.count();
    }

    @Transactional
    public void clearAllCountries() {
        countryRepository.deleteAll();
    }

    public Country getCountryByCode(String code) {
        try {
            Country c = countryRepository.findByCountryCodeIgnoreCase(code).orElse(null);
            if (c == null) {
                throw new RuntimeException("Country not found in database: " + code);
            } else {
                return c;
            }

        } catch (RestClientException e) {
            throw new RuntimeException("Failed to fetch country: " + code, e);
        }
    }

    private boolean isValidCountry(CountryAPI apiResponse) {
        return apiResponse != null &&
               apiResponse.getCode() != null &&
               !apiResponse.getCode().isEmpty() &&
               apiResponse.getName() != null &&
               apiResponse.getName().getCommon() != null;
    }

    /**
     * Convert country name to 3-digit ISO code
     * @param countryName Full country name (e.g., "Singapore", "China")
     * @return 3-digit ISO code (e.g., "SGP", "CHN") or null if not found
     */
    public String convertCountryNameToIso3(String countryName) {
        if (countryName == null || countryName.trim().isEmpty()) {
            return null;
        }

        String normalizedName = countryName.trim();

        // Check cache first
        String cachedCode = countryNameToIso3Cache.get(normalizedName.toLowerCase());
        if (cachedCode != null) {
            log.debug("Found cached conversion: {} -> {}", normalizedName, cachedCode);
            return cachedCode;
        }

        // Check database
        Country country = countryRepository.findByCountryNameIgnoreCase(normalizedName).orElse(null);
        if (country != null && country.getIso3Code() != null) {
            // Cache the result
            countryNameToIso3Cache.put(normalizedName.toLowerCase(), country.getIso3Code());
            return country.getIso3Code();
        }

        // Try API lookup as fallback
        try {
            String apiUrl = "https://restcountries.com/v3.1/name/" + normalizedName + "?fullText=true&fields=cca3";
            String response = restTemplate.getForObject(apiUrl, String.class);

            if (response != null && response.contains("cca3")) {
                // Extract the 3-digit code from JSON response
                String iso3Code = extractIso3FromApiResponse(response);
                if (iso3Code != null) {
                    // Cache the result
                    countryNameToIso3Cache.put(normalizedName.toLowerCase(), iso3Code);
                    log.info("API lookup successful: {} -> {}", normalizedName, iso3Code);
                    return iso3Code;
                }
            }
        } catch (Exception e) {
            log.warn("API lookup failed for country name: {}", normalizedName);
        }

        log.warn("Could not convert country name to ISO3 code: {}", normalizedName);
        return null;
    }

    /**
     * Convert country name to 2-digit ISO code
     * @param countryName Full country name (e.g., "Singapore", "China") or ISO3 code (e.g., "SGP", "CHN")
     * @return 2-digit ISO code (e.g., "SG", "CN") or null if not found
     */
    public String convertCountryNameToIso2(String countryName) {
        if (countryName == null || countryName.trim().isEmpty()) {
            return null;
        }

        String normalizedInput = countryName.trim();

        // Check if input is already a 2-digit ISO code
        if (normalizedInput.length() == 2 && normalizedInput.matches("[A-Z]{2}")) {
            log.debug("Input is already ISO2 code: {}", normalizedInput);
            return normalizedInput;
        }

        // Check if input is a 3-digit ISO code and convert to 2-digit
        if (normalizedInput.length() == 3 && normalizedInput.matches("[A-Z]{3}")) {
            String iso2Code = convertIso3ToIso2(normalizedInput);
            if (iso2Code != null) {
                log.debug("Converted ISO3 to ISO2: {} -> {}", normalizedInput, iso2Code);
                return iso2Code;
            }
        }

        // Check cache first (for country names)
        String cachedCode = countryNameToIso2Cache.get(normalizedInput.toLowerCase());
        if (cachedCode != null) {
            log.debug("Found cached ISO2 conversion: {} -> {}", normalizedInput, cachedCode);
            return cachedCode;
        }

        // Check database by country name
        Country country = countryRepository.findByCountryNameIgnoreCase(normalizedInput).orElse(null);
        if (country != null && country.getCountryCode() != null) {
            // Cache the result
            countryNameToIso2Cache.put(normalizedInput.toLowerCase(), country.getCountryCode());
            return country.getCountryCode();
        }

        // Try API lookup as fallback
        try {
            String apiUrl = "https://restcountries.com/v3.1/name/" + normalizedInput + "?fullText=true&fields=cca2";
            String response = restTemplate.getForObject(apiUrl, String.class);

            if (response != null && response.contains("cca2")) {
                // Extract the 2-digit code from JSON response
                String iso2Code = extractIso2FromApiResponse(response);
                if (iso2Code != null) {
                    // Cache the result
                    countryNameToIso2Cache.put(normalizedInput.toLowerCase(), iso2Code);
                    log.info("API lookup successful for ISO2: {} -> {}", normalizedInput, iso2Code);
                    return iso2Code;
                }
            }
        } catch (Exception e) {
            log.warn("API lookup failed for country name: {}", normalizedInput);
        }

        log.warn("Could not convert country name to ISO2 code: {}", normalizedInput);
        return null;
    }

    /**
     * Convert ISO3 code to ISO2 code using a mapping
     */
    private String convertIso3ToIso2(String iso3Code) {
        // Create a mapping of common ISO3 to ISO2 codes
        Map<String, String> iso3ToIso2Map = new HashMap<>();
        iso3ToIso2Map.put("USA", "US");
        iso3ToIso2Map.put("CHN", "CN");
        iso3ToIso2Map.put("SGP", "SG");
        iso3ToIso2Map.put("MYS", "MY");
        iso3ToIso2Map.put("JPN", "JP");
        iso3ToIso2Map.put("KOR", "KR");
        iso3ToIso2Map.put("THA", "TH");
        iso3ToIso2Map.put("VNM", "VN");
        iso3ToIso2Map.put("IDN", "ID");
        iso3ToIso2Map.put("PHL", "PH");
        iso3ToIso2Map.put("IND", "IN");
        iso3ToIso2Map.put("AUS", "AU");
        iso3ToIso2Map.put("NZL", "NZ");
        iso3ToIso2Map.put("GBR", "GB");
        iso3ToIso2Map.put("DEU", "DE");
        iso3ToIso2Map.put("FRA", "FR");
        iso3ToIso2Map.put("ITA", "IT");
        iso3ToIso2Map.put("ESP", "ES");
        iso3ToIso2Map.put("NLD", "NL");
        iso3ToIso2Map.put("BEL", "BE");
        iso3ToIso2Map.put("CHE", "CH");
        iso3ToIso2Map.put("CAN", "CA");
        iso3ToIso2Map.put("MEX", "MX");
        iso3ToIso2Map.put("BRA", "BR");
        iso3ToIso2Map.put("ARG", "AR");
        iso3ToIso2Map.put("CHL", "CL");

        String iso2Code = iso3ToIso2Map.get(iso3Code.toUpperCase());
        if (iso2Code != null) {
            return iso2Code;
        }

        // If not in our mapping, try API lookup
        try {
            String apiUrl = "https://restcountries.com/v3.1/alpha/" + iso3Code + "?fields=cca2";
            String response = restTemplate.getForObject(apiUrl, String.class);

            if (response != null && response.contains("cca2")) {
                return extractIso2FromApiResponse(response);
            }
        } catch (Exception e) {
            log.warn("API lookup failed for ISO3 code: {}", iso3Code);
        }

        return null;
    }

    /**
     * Initialize cache with common country name mappings
     */
    private void initializeCountryNameCache() {
        Map<String, String> commonMappings = new HashMap<>();

        // Common country name mappings
        commonMappings.put("singapore", "SGP");
        commonMappings.put("china", "CHN");
        commonMappings.put("united states", "USA");
        commonMappings.put("united states of america", "USA");
        commonMappings.put("malaysia", "MYS");
        commonMappings.put("japan", "JPN");
        commonMappings.put("south korea", "KOR");
        commonMappings.put("korea, republic of", "KOR");
        commonMappings.put("thailand", "THA");
        commonMappings.put("vietnam", "VNM");
        commonMappings.put("indonesia", "IDN");
        commonMappings.put("philippines", "PHL");
        commonMappings.put("india", "IND");
        commonMappings.put("australia", "AUS");
        commonMappings.put("new zealand", "NZL");
        commonMappings.put("united kingdom", "GBR");
        commonMappings.put("germany", "DEU");
        commonMappings.put("france", "FRA");
        commonMappings.put("italy", "ITA");
        commonMappings.put("spain", "ESP");
        commonMappings.put("netherlands", "NLD");
        commonMappings.put("belgium", "BEL");
        commonMappings.put("switzerland", "CHE");
        commonMappings.put("canada", "CAN");
        commonMappings.put("mexico", "MEX");
        commonMappings.put("brazil", "BRA");
        commonMappings.put("argentina", "ARG");
        commonMappings.put("chile", "CHL");

        countryNameToIso3Cache.putAll(commonMappings);
        log.info("Initialized country name cache with {} mappings", commonMappings.size());
    }

    /**
     * Initialize cache with common country name to ISO2 mappings
     */
    private void initializeCountryNameToIso2Cache() {
        Map<String, String> commonMappings = new HashMap<>();

        // Common country name to ISO2 mappings
        commonMappings.put("singapore", "SG");
        commonMappings.put("china", "CN");
        commonMappings.put("united states", "US");
        commonMappings.put("united states of america", "US");
        commonMappings.put("malaysia", "MY");
        commonMappings.put("japan", "JP");
        commonMappings.put("south korea", "KR");
        commonMappings.put("korea, republic of", "KR");
        commonMappings.put("thailand", "TH");
        commonMappings.put("vietnam", "VN");
        commonMappings.put("indonesia", "ID");
        commonMappings.put("philippines", "PH");
        commonMappings.put("india", "IN");
        commonMappings.put("australia", "AU");
        commonMappings.put("new zealand", "NZ");
        commonMappings.put("united kingdom", "GB");
        commonMappings.put("germany", "DE");
        commonMappings.put("france", "FR");
        commonMappings.put("italy", "IT");
        commonMappings.put("spain", "ES");
        commonMappings.put("netherlands", "NL");
        commonMappings.put("belgium", "BE");
        commonMappings.put("switzerland", "CH");
        commonMappings.put("canada", "CA");
        commonMappings.put("mexico", "MX");
        commonMappings.put("brazil", "BR");
        commonMappings.put("argentina", "AR");
        commonMappings.put("chile", "CL");

        countryNameToIso2Cache.putAll(commonMappings);
        log.info("Initialized country name to ISO2 cache with {} mappings", commonMappings.size());
    }

    /**
     * Extract ISO3 code from API response JSON
     */
    private String extractIso3FromApiResponse(String jsonResponse) {
        try {
            // Simple JSON parsing to extract cca3 value
            if (jsonResponse.contains("\"cca3\":")) {
                int start = jsonResponse.indexOf("\"cca3\":\"") + 8;
                int end = jsonResponse.indexOf("\"", start);
                if (start > 7 && end > start) {
                    return jsonResponse.substring(start, end);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing API response: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extract ISO2 code from API response JSON
     */
    private String extractIso2FromApiResponse(String jsonResponse) {
        try {
            // Simple JSON parsing to extract cca2 value
            if (jsonResponse.contains("\"cca2\":")) {
                int start = jsonResponse.indexOf("\"cca2\":\"") + 8;
                int end = jsonResponse.indexOf("\"", start);
                if (start > 7 && end > start) {
                    return jsonResponse.substring(start, end);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing API response for ISO2: {}", e.getMessage());
        }
        return null;
    }

    private Country mapToCountry(CountryAPI apiResponse) {
        try {
            String code = apiResponse.getCode() != null ? apiResponse.getCode() : "UNKNOWN";
            String name = (apiResponse.getName() != null && apiResponse.getName().getCommon() != null) 
                ? apiResponse.getName().getCommon() : "Unknown";
            String iso3Code = apiResponse.getIso3Code() != null ? apiResponse.getIso3Code() : null;

            return new Country(code, name, iso3Code);

        } catch (Exception e) {
            System.err.println("Error mapping country: " + e.getMessage());
            return new Country("ERR", "Error Country", null);
        }
    }

    private String extractFirstCurrency(Object currencies) {
        try {
            if (currencies == null) {
                return "Unknown";
            }

            // Handle if currencies is a Map (which it usually is from JSON)
            if (currencies instanceof Map) {
                Map<?, ?> currencyMap = (Map<?, ?>) currencies;
                if (!currencyMap.isEmpty()) {
                    // Get the first currency code (key)
                    return currencyMap.keySet().iterator().next().toString();
                }
            }

            // Fallback for string representation
            String currencyStr = currencies.toString();
            if (currencyStr.contains("=")) {
                return currencyStr.split("=")[0].replace("{", "").trim();
            }

            return "Unknown";
        } catch (Exception e) {
            System.err.println("Error extracting currency: " + e.getMessage());
            return "Unknown";
        }
    }

    public boolean deleteCountryByCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Country code cannot be null or empty");
        }
        if (!countryRepository.existsByCountryCodeIgnoreCase(code)) {
            return false; // Country not found
        }

        Country country = countryRepository.findByCountryCode(code).orElse(null);
        assert country != null;
        countryRepository.delete(country);
        return true;
    }


    public Country createCountry(String code, String name) {
        if (code == null || code.isEmpty() || name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Country code and name cannot be null or empty");
        }
        if (countryRepository.existsByCountryCode(code)) {
            throw new IllegalArgumentException("Country with code " + code + " already exists");
        }

        Country country = new Country(code.toUpperCase(), name);
        return countryRepository.save(country);
    }

    public Country get3DigitCountryCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Country code cannot be null or empty");
        }

        //SELECT * FROM COUNTRIES WHERE COUNTRY_CODE = CODE (case insensitive);
        return countryRepository.findByCountryCodeIgnoreCase(code).orElse(null);
    }
    public Country updateCountry(String code, String name) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Country code cannot be null or empty");
        }

        //SELECT * FROM COUNTRIES WHERE COUNTRY_CODE = CODE (case insensitive);
        Country country = countryRepository.findByCountryCodeIgnoreCase(code).orElse(null);
        if (country == null) {
            return null; // Country not found
        }
        if (name != null && !name.isEmpty()) {
            country.setCountryName(name);
        }
        return countryRepository.save(country);
    }



}
