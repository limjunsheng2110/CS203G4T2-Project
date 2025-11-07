package com.cs203.tariffg4t2.controller;

import com.cs203.tariffg4t2.model.basic.Country;
import com.cs203.tariffg4t2.service.basic.CountryService;
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

    // Get countries from database
    @GetMapping("/all")
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

    //CREATE
    @PostMapping("/add")
    public ResponseEntity<?> addCountry(
            @RequestParam String code,
            @RequestParam String name
    ) {
        try {
            Country newCountry = countryService.createCountry(code, name);
            return ResponseEntity.status(HttpStatus.CREATED).body(newCountry);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid input: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error in addCountry: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error adding country: " + e.getMessage());
        }
    }


    //DELETE
    @DeleteMapping("/delete/{code}")
    public ResponseEntity<?> deleteCountryByCode(@PathVariable String code) {
        try {
            boolean deleted = countryService.deleteCountryByCode(code);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Country not found: " + code);
            }
            return ResponseEntity.ok("Country deleted: " + code);
        } catch (Exception e) {
            System.err.println("Error in deleteCountryByCode: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting country: " + e.getMessage());
        }
    }


    //READ
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

    //UPDATE
    @PutMapping("/update/{code}")
    public ResponseEntity<?> updateCountry(
            @PathVariable String code,
            @RequestParam String name
    ) {
        try {
            Country updatedCountry = countryService.updateCountry(code, name);
            if (updatedCountry == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Country not found: " + code);
            }
            return ResponseEntity.ok(updatedCountry);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid input: " + e.getMessage());
        }

    }


    //---------------------API Fetching and Non-CRUD endpoints---------------------//

   @GetMapping
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



}