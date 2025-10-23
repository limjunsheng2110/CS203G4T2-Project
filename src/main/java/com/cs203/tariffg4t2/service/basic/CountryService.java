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

@Slf4j
@Service
public class CountryService {

    private final RestTemplate restTemplate;
    private static final String COUNTRIES_API_URL = "https://restcountries.com/v3.1/all?fields=cca2,name,region,currencies";

    @Autowired
    private CountryRepository countryRepository;

    public CountryService() {
        this.restTemplate = new RestTemplate();
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

    private Country mapToCountry(CountryAPI apiResponse) {
        try {
            String code = apiResponse.getCode() != null ? apiResponse.getCode() : "UNKNOWN";
            String name = (apiResponse.getName() != null && apiResponse.getName().getCommon() != null) 
                ? apiResponse.getName().getCommon() : "Unknown";
            
            return new Country(code, name);
            
        } catch (Exception e) {
            System.err.println("Error mapping country: " + e.getMessage());
            return new Country("ERR", "Error Country");
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
