package com.cs205.tariffg4t2.controller;

import com.cs205.tariffg4t2.model.basic.Country;
import com.cs205.tariffg4t2.service.CountryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
public class CountryController {

    @Autowired
    private CountryService countryService;

    @GetMapping
    public List<Country> getAllCountries() {
        return countryService.getAllCountries();
    }

    @GetMapping("/{code}")
    public Country getCountryByCode(@PathVariable String code) {
        Country country = countryService.getCountryByCode(code);
        if (country == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Country not found: " + code);
        }
        return country;
    }

    // to test endpoint to see raw API response
    @GetMapping("/test-raw")
    public String testRawApi() {
        return "API integration is working! Try /api/countries or /api/countries/US";
    }
}