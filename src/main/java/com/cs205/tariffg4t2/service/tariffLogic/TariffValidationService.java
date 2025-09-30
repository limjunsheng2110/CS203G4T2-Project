package com.cs205.tariffg4t2.service.tariffLogic;

import com.cs205.tariffg4t2.dto.request.TariffCalculationRequest;
import com.cs205.tariffg4t2.repository.basic.CountryRepository;
import com.cs205.tariffg4t2.model.basic.Country;
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


    public List<String> validateTariffRequest(TariffCalculationRequest request) {
        List<String> errors = new ArrayList<>();

        // required fields
        if (isBlank(request.getHomeCountry())) errors.add("Home country is required");
        if (isBlank(request.getDestinationCountry())) errors.add("Destination country is required");
        if (isBlank(request.getProductName())) errors.add("Product name is required");
        if (request.getProductValue() == null || request.getProductValue().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Product value must be greater than zero");
        }

        // Short-circuit if requireds already failed
        if (!errors.isEmpty()) return errors;

        // Resolve + normalize countries to alpha-2
        Optional<String> homeCode = resolveToAlpha2(request.getHomeCountry());
        Optional<String> destCode = resolveToAlpha2(request.getDestinationCountry());

        if (homeCode.isEmpty()) {
            errors.add("Unknown home country (not found by code or name)");
        } else {
            request.setHomeCountry(homeCode.get());  // normalize to alpha-2
        }

        if (destCode.isEmpty()) {
            errors.add("Unknown destination country (not found by code or name)");
        } else {
            request.setDestinationCountry(destCode.get()); // normalize to alpha-2
        }

        // HS code (optional): keep your old rule if you like
        if (request.getHsCode() != null && !request.getHsCode().isBlank()) {
            if (!request.getHsCode().matches("^\\d{6,10}$")) {
                errors.add("Invalid HS code format: must be 6 to 10 digits");
            }
        }

        boolean hasValue = request.getProductValue() != null && request.getProductValue().compareTo(BigDecimal.ZERO) > 0;
        boolean hasQty   = request.getQuantity() != null && request.getQuantity().compareTo(BigDecimal.ZERO) > 0;
        boolean hasUnit  = request.getUnit() != null && !request.getUnit().isBlank();

        if (!hasValue && !(hasQty && hasUnit)) {
            errors.add("Provide either productValue > 0, or (quantity > 0 and unit).");
        }

        if (!hasValue && hasQty && !hasUnit) {
            errors.add("Provide either productValue > 0, or (quantity > 0 and unit).");
        }
        if (!hasValue && hasUnit && !hasQty) {
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

    public boolean isValidRequest(TariffCalculationRequest request) {
        return validateTariffRequest(request).isEmpty();
    }
}

