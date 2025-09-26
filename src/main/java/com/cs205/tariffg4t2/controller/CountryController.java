package com.cs205.tariffg4t2.controller;

import com.cs205.tariffg4t2.model.basic.Country;
import com.cs205.tariffg4t2.service.country.CountryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
public class CountryController {

    @Autowired
    private CountryService countryService;

    // Get countries from API
    @GetMapping("/all")
    public ResponseEntity<?> getAllCountries() {
        try {
            List<Country> countries = countryService.getAllCountries();
            return ResponseEntity.ok(countries);
        } catch (Exception e) {
            System.err.println("Error in getAllCountries: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching countries: " + e.getMessage());
        }
    }

    // Get countries from database
    @GetMapping("/db/all")
    public ResponseEntity<?> getAllCountriesFromDatabase() {
        try {
            List<Country> countries = countryService.getAllCountriesFromDatabase();
            return ResponseEntity.ok(countries);
        } catch (Exception e) {
            System.err.println("Error in getAllCountriesFromDatabase: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching countries from database: " + e.getMessage());
        }
    }

    // Populate database with countries from API
    @PostMapping("/populate")
    public ResponseEntity<?> populateCountriesDatabase() {
        try {
            String result = countryService.populateCountriesDatabase();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Error in populateCountriesDatabase: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error populating database: " + e.getMessage());
        }
    }

    // Alternative populate method with batch processing
    @PostMapping("/populate-batch")
    public ResponseEntity<?> populateCountriesDatabaseBatch() {
        try {
            String result = countryService.populateCountriesDatabaseBatch();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Error in populateCountriesDatabaseBatch: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error populating database: " + e.getMessage());
        }
    }

    // Clear all countries from database
    @DeleteMapping("/db/clear")
    public ResponseEntity<?> clearCountriesDatabase() {
        try {
            countryService.clearAllCountries();
            return ResponseEntity.ok("All countries cleared from database");
        } catch (Exception e) {
            System.err.println("Error in clearCountriesDatabase: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error clearing database: " + e.getMessage());
        }
    }

    // Get count of countries in database
    @GetMapping("/db/count")
    public ResponseEntity<?> getCountriesCount() {
        try {
            long count = countryService.getCountriesCount();
            return ResponseEntity.ok("Countries in database: " + count);
        } catch (Exception e) {
            System.err.println("Error in getCountriesCount: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error getting count: " + e.getMessage());
        }
    }

    @GetMapping("/{code}")
    public ResponseEntity<?> getCountryByCode(@PathVariable String code) {
        try {
            Country country = countryService.getCountryByCode(code);
            if (country == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Country not found: " + code);
            }
            return ResponseEntity.ok(country);
        } catch (Exception e) {
            System.err.println("Error in getCountryByCode: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching country: " + e.getMessage());
        }
    }

    // to test endpoint to see raw API response
    @GetMapping("/test-raw")
    public String testRawApi() {
        return "API integration is working! Try /api/countries or /api/countries/US";
    }
}