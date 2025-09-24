package com.cs205.tariffg4t2.service.tariffLogic;

import com.cs205.tariffg4t2.dto.request.TariffCalculationRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class TariffValidationService {

    public List<String> validateTariffRequest(TariffCalculationRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.getHomeCountry() == null || request.getHomeCountry().trim().isEmpty()) {
            errors.add("Home country is required");
        }

        if (request.getDestinationCountry() == null || request.getDestinationCountry().trim().isEmpty()) {
            errors.add("Destination country is required");
        }

        if (request.getProductName() == null || request.getProductName().trim().isEmpty()) {
            errors.add("Product name is required");
        }

        if (request.getProductValue() == null || request.getProductValue().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Product value must be greater than zero");
        }

        // Validate country codes (ISO 3166-1 alpha-3)
        if (request.getHomeCountry() != null && !isValidCountryCode(request.getHomeCountry())) {
            errors.add("Invalid home country code format");
        }

        if (request.getDestinationCountry() != null && !isValidCountryCode(request.getDestinationCountry())) {
            errors.add("Invalid destination country code format");
        }

        // Validate HS code format if provided
        if (request.getHsCode() != null && !isValidHsCode(request.getHsCode())) {
            errors.add("Invalid HS code format");
        }

        return errors;
    }

    private boolean isValidCountryCode(String countryCode) {
        // Basic validation for 3-letter country codes
        return countryCode.matches("^[A-Z]{3}$");
    }

    private boolean isValidHsCode(String hsCode) {
        // Basic validation for HS codes (6-10 digits)
        return hsCode.matches("^\\d{6,10}$");
    }

    public boolean isValidRequest(TariffCalculationRequest request) {
        return validateTariffRequest(request).isEmpty();
    }
}

