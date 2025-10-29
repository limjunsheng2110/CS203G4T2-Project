package com.cs203.tariffg4t2.service.tariffLogic;

import com.cs203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.cs203.tariffg4t2.repository.basic.CountryRepository;
import com.cs203.tariffg4t2.model.basic.Country;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class TariffValidationService {

    @Autowired
    private CountryRepository countryRepository;


    public List<String> validateTariffRequest(TariffCalculationRequestDTO request) {
        List<String> errors = new ArrayList<>();

        // required fields
        if (isBlank(request.getImportingCountry())) errors.add("Importing country is required");
        if (isBlank(request.getExportingCountry())) errors.add("Exporting country is required");
        if (request.getProductValue() == null || request.getProductValue().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Product value must be greater than zero");
        }

        if (request.getWeight() == null || request.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Weight must be greater than zero");
        }

        if (isBlank(request.getShippingMode())) errors.add("Shipping mode is required");
        if (request.getHeads() == null || request.getHeads() < 0) {
            errors.add("Heads must be zero or a positive integer");
        }



        // Short-circuit if requireds already failed
        if (!errors.isEmpty()) return errors;

        // Resolve + normalize countries to alpha-2
        Optional<String> importCode = resolveToAlpha2(request.getImportingCountry());
        Optional<String> exportCode = resolveToAlpha2(request.getExportingCountry());

        if (importCode.isEmpty()) {
            errors.add("Unknown home country (not found by code or name)");
        } else {
            request.setImportingCountry(importCode.get());  // normalize to alpha-2
        }

        if (exportCode.isEmpty()) {
            errors.add("Unknown destination country (not found by code or name)");
        } else {
            request.setExportingCountry(exportCode.get()); // normalize to alpha-2
        }

        // HS code (optional): keep your old rule if you like
        if (request.getHsCode() != null && !request.getHsCode().isBlank()) {
            if (!request.getHsCode().matches("^\\d{6,10}$")) {
                errors.add("Invalid HS code format: must be 6 to 10 digits");
            }
        }

        boolean hasValue = request.getProductValue() != null && request.getProductValue().compareTo(BigDecimal.ZERO) > 0;
        boolean hasQty   = request.getWeight() != null && request.getWeight().compareTo(BigDecimal.ZERO) > 0;

        if (!hasValue && !hasQty) {
            errors.add("Provide either productValue > 0, or (quantity > 0 and unit).");
        }

        return errors;
    }

    /** Try to resolve user input to an alpha-2 country code using DB. */
    private Optional<String> resolveToAlpha2(String input) {
        if (isBlank(input)) return Optional.empty();

        String s = input.trim();

        // Case 1: looks like alpha-2 code => check DB by code
        if (s.matches("(?i)^[A-Z]{2}$")) {
            System.out.println("Looking up country by code: " + s);
            return countryRepository.findByCountryCodeIgnoreCase(s).map(Country::getCountryCode);
        }

        // Case 2: treat as full name => check DB by name (case-insensitive exact match)
        return countryRepository.findByCountryNameIgnoreCase(s).map(Country::getCountryCode);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public boolean isValidRequest(TariffCalculationRequestDTO request) {
        return validateTariffRequest(request).isEmpty();
    }
}

