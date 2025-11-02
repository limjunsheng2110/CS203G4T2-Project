package com.cs203.tariffg4t2.service.tariffLogic;

import com.cs203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.cs203.tariffg4t2.repository.basic.CountryRepository;
import com.cs203.tariffg4t2.model.basic.Country;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class TariffValidationService {

    @Autowired
    private CountryRepository countryRepository;

    // Simple in-memory cache to avoid repeated DB lookups
    private final Map<String, Optional<String>> countryCache = new ConcurrentHashMap<>();

    public List<String> validateTariffRequest(TariffCalculationRequestDTO request) {
        List<String> errors = new ArrayList<>();

        // Null check first
        if (request == null) {
            errors.add("Request cannot be null");
            return errors;
        }

        // Initialize tracking lists if null
        if (request.getMissingFields() == null) {
            request.setMissingFields(new ArrayList<>());
        }
        if (request.getDefaultedFields() == null) {
            request.setDefaultedFields(new ArrayList<>());
        }

        // Handle missing required fields with defaults
        if (isBlank(request.getImportingCountry())) {
            request.getMissingFields().add("importingCountry");
            errors.add("Importing country is required");
        }

        if (isBlank(request.getExportingCountry())) {
            request.getMissingFields().add("exportingCountry");
            errors.add("Exporting country is required");
        }

        if (isBlank(request.getHsCode())) {
            request.getMissingFields().add("hsCode");
            errors.add("HS code is required");
        }

        // Set defaults for missing values and track them
        if (request.getProductValue() == null) {
            request.getMissingFields().add("productValue");
            request.setProductValue(BigDecimal.valueOf(100.00));
            request.getDefaultedFields().add("productValue (defaulted to $100.00)");
        } else if (request.getProductValue().compareTo(BigDecimal.ZERO) <= 0) {
            request.setProductValue(BigDecimal.valueOf(100.00));
            request.getDefaultedFields().add("productValue (corrected from non-positive to $100.00)");
        }

        if (request.getRooEligible() == null) {
            request.getMissingFields().add("rooEligible");
            request.setRooEligible(false);
            request.getDefaultedFields().add("rooEligible (defaulted to false)");
        }

        if (isBlank(request.getShippingMode())) {
            request.getMissingFields().add("shippingMode");
            request.setShippingMode("SEA");
            request.getDefaultedFields().add("shippingMode (defaulted to SEA)");
        }

        boolean hasHeads = request.getHeads() != null && request.getHeads() > 0;
        boolean hasWeight = request.getWeight() != null && request.getWeight().compareTo(BigDecimal.ZERO) > 0;

        if (!hasHeads && !hasWeight) {
            request.getMissingFields().add("heads/weight");
            request.setHeads(1);
            request.setWeight(BigDecimal.ONE);
            request.getDefaultedFields().add("heads (defaulted to 1)");
            request.getDefaultedFields().add("weight (defaulted to 1.0 kg)");
        } else {
            if (request.getHeads() != null && request.getHeads() < 0) {
                request.setHeads(1);
                request.getDefaultedFields().add("heads (corrected from negative to 1)");
            }
            if (request.getWeight() != null && request.getWeight().compareTo(BigDecimal.ZERO) < 0) {
                request.setWeight(BigDecimal.ONE);
                request.getDefaultedFields().add("weight (corrected from negative to 1.0 kg)");
            }
        }

        if (request.getFreight() == null) {
            request.getMissingFields().add("freight");
            request.setFreight(BigDecimal.ZERO);
            request.getDefaultedFields().add("freight (defaulted to $0.00)");
        }

        if (request.getInsurance() == null) {
            request.getMissingFields().add("insurance");
            request.setInsurance(BigDecimal.ZERO);
            request.getDefaultedFields().add("insurance (defaulted to $0.00)");
        }

        // Only proceed with country validation if we have country codes
        if (!isBlank(request.getImportingCountry()) && !isBlank(request.getExportingCountry())) {
            // Resolve + normalize countries to alpha-2
            Optional<String> importCode = resolveToAlpha2Cached(request.getImportingCountry());
            Optional<String> exportCode = resolveToAlpha2Cached(request.getExportingCountry());

            if (importCode.isEmpty()) {
                errors.add("Unknown importing country (not found by code or name): " + request.getImportingCountry());
            } else {
                request.setImportingCountry(importCode.get());
            }

            if (exportCode.isEmpty()) {
                errors.add("Unknown exporting country (not found by code or name): " + request.getExportingCountry());
            } else {
                request.setExportingCountry(exportCode.get());
            }
        }

        // HS code format validation
        if (request.getHsCode() != null && !request.getHsCode().isBlank()) {
            String hsCode = request.getHsCode().trim();
            if (!hsCode.matches("^\\d{6,10}$")) {
                String cleanedHsCode = hsCode.replaceAll("\\D", "");
                if (cleanedHsCode.length() >= 6 && cleanedHsCode.length() <= 10) {
                    request.setHsCode(cleanedHsCode);
                    request.getDefaultedFields().add("hsCode (cleaned non-digit characters)");
                } else {
                    errors.add("Invalid HS code format: must be 6 to 10 digits (got: " + hsCode + ")");
                }
            }
        }

        return errors;
    }

    /** Cached version to avoid repeated DB lookups */
    private Optional<String> resolveToAlpha2Cached(String input) {
        if (isBlank(input)) return Optional.empty();

        String key = input.trim().toUpperCase();
        return countryCache.computeIfAbsent(key, k -> resolveToAlpha2(input));
    }

    /** Try to resolve user input to an alpha-2 country code using DB. */
    public Optional<String> resolveToAlpha2(String input) {
        if (isBlank(input)) return Optional.empty();

        String s = input.trim();

        // Case 1: looks like alpha-2 code => check DB by code
        if (s.matches("(?i)^[A-Z]{2}$")) {
            Optional<Country> country = countryRepository.findByCountryCodeIgnoreCase(s);
            if (country.isPresent()) {
                return Optional.of(country.get().getCountryCode());
            }
        }

        // Case 2: treat as full name => check DB by name
        Optional<Country> country = countryRepository.findByCountryNameIgnoreCase(s);
        return country.map(Country::getCountryCode);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public boolean isValidRequest(TariffCalculationRequestDTO request) {
        return validateTariffRequest(request).isEmpty();
    }
}