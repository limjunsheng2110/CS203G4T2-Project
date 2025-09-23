package com.cs205.tariffg4t2.controller;

import com.cs205.tariffg4t2.dto.request.TariffCalculationRequest;
import com.cs205.tariffg4t2.dto.response.TariffCalculationResponse;
import com.cs205.tariffg4t2.dto.response.TariffCalculationResult;

import com.cs205.tariffg4t2.service.TariffCalculatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/tariff")
public class TariffController {

    @Autowired
    private TariffCalculatorService tariffService;

    @GetMapping("/calculate")
    public ResponseEntity<TariffCalculationResponse> calculateTariff(
            @RequestParam String homeCountry,
            @RequestParam String destinationCountry,
            @RequestParam String productCategory,
            @RequestParam BigDecimal productValue,
            @RequestParam(required = false) String hsCode,
            @RequestParam(required = false) String tradeAgreement) {

        try {
            // Validate input parameters
            ResponseEntity<TariffCalculationResponse> validationError = validateInputParameters(
                    homeCountry, destinationCountry, productCategory, productValue);
            if (validationError != null) {
                return validationError;
            }

            // Create calculation request
            TariffCalculationRequest request = TariffCalculationRequest.builder()
                    .homeCountry(homeCountry.trim().toUpperCase())
                    .destinationCountry(destinationCountry.trim().toUpperCase())
                    .productCategory(productCategory)
                    .productValue(productValue)
                    .hsCode(hsCode)
                    .tradeAgreement(tradeAgreement)
                    .build();

            // Calculate tariff
            TariffCalculationResult result = tariffService.calculateTariff(request);

            return ResponseEntity.ok(new TariffCalculationResponse("Success", result));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new TariffCalculationResponse("Internal server error", null));
        }
    }


    private ResponseEntity<TariffCalculationResponse> validateInputParameters(
            String homeCountry, String destinationCountry, String productCategory, BigDecimal productValue) {

        if (homeCountry == null || homeCountry.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new TariffCalculationResponse("Home country is required", null));
        }

        if (destinationCountry == null || destinationCountry.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new TariffCalculationResponse("Destination country is required", null));
        }

        if (productCategory == null || productCategory.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new TariffCalculationResponse("Product category is required", null));
        }


        return null; // No validation errors
    }
}