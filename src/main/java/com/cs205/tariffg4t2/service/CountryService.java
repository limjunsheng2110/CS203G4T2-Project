package com.cs205.tariffg4t2.service;


import com.cs205.tariffg4t2.model.basic.Country;
import com.cs205.tariffg4t2.model.CountryAPI;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CountryService {

    private final RestTemplate restTemplate;
    private static final String COUNTRIES_API_URL = "https://restcountries.com/v3.1/all";

    public CountryService() {
        this.restTemplate = new RestTemplate();
    }

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
                    .map(this::mapToCountry)
                    .collect(Collectors.toList());

        } catch (RestClientException e) {
            throw new RuntimeException("Failed to fetch countries from API", e);
        }
    }

    public Country getCountryByCode(String code) {
        try {
            String url = "https://restcountries.com/v3.1/alpha/" + code;
            CountryAPI[] apiResponses = restTemplate.getForObject(
                    url,
                    CountryAPI[].class
            );

            if (apiResponses == null || apiResponses.length == 0) {
                return null;
            }

            return mapToCountry(apiResponses[0]);

        } catch (RestClientException e) {
            throw new RuntimeException("Failed to fetch country: " + code, e);
        }
    }

    private Country mapToCountry(CountryAPI apiResponse) {
        String name = (apiResponse.getName() != null) ?
                apiResponse.getName().getCommon() : "Unknown";

        // Extract first currency (simplified approach)
        String currency = extractFirstCurrency(apiResponse.getCurrencies());

        return new Country(
                apiResponse.getCode(),
                name,
                apiResponse.getRegion(),
                currency
        );
    }

    private String extractFirstCurrency(Object currencies) {
        // Simplified - you might want more sophisticated currency extraction
        if (currencies != null) {
            String currencyStr = currencies.toString();
            // Basic extraction - this could be improved
            if (currencyStr.contains("=")) {
                return currencyStr.split("=")[0].replace("{", "").trim();
            }
        }
        return "Unknown";
    }
}
