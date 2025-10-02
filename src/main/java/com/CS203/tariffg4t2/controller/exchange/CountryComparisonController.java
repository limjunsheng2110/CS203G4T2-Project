package com.CS203.tariffg4t2.controller.exchange;

import com.CS203.tariffg4t2.dto.request.CountryComparisonRequest;
import com.CS203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.CS203.tariffg4t2.dto.response.CountryComparisonResult;
import com.CS203.tariffg4t2.dto.response.TariffCalculationResponseDTO;
import com.CS203.tariffg4t2.dto.response.TariffCalculationResultDTO;
import com.CS203.tariffg4t2.service.exchange.ExchangeRateService;
import com.CS203.tariffg4t2.service.exchange.CountryComparisonService;
import com.CS203.tariffg4t2.service.tariffLogic.TariffCalculatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/comparison")
@CrossOrigin(origins = "*")
public class CountryComparisonController {
    
    @Autowired
    private TariffCalculatorService tariffService;
    
    @Autowired
    private CountryComparisonService comparisonService;
    
    @Autowired
    private ExchangeRateService exchangeRateService;
    
    /**
     * Single tariff calculation with currency conversion
     */
    @GetMapping("/calculate")
    public ResponseEntity<TariffCalculationResponseDTO> calculateTariff(
            @RequestParam String importingCountry,
            @RequestParam String exportingCountry,
            @RequestParam(required = false) BigDecimal productValue,
            @RequestParam(required = false) BigDecimal weight,
            @RequestParam(required = false) Integer heads,
            @RequestParam(required = false) String shippingMode,
            @RequestParam String hsCode) {
        try {
            TariffCalculationRequestDTO request = TariffCalculationRequestDTO.builder()
                .importingCountry(importingCountry.trim().toUpperCase())
                .exportingCountry(exportingCountry.trim().toUpperCase())
                .productValue(productValue)
                .weight(weight)
                .heads(heads != null ? heads : 1)
                .shippingMode(shippingMode != null ? shippingMode.trim() : null)
                .hsCode(hsCode)
                .build();
            
            TariffCalculationResultDTO result = tariffService.calculateTariff(request);
            return ResponseEntity.ok(new TariffCalculationResponseDTO("Success", result));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new TariffCalculationResponseDTO(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new TariffCalculationResponseDTO("Internal server error", null));
        }
    }
    
    /**
     * NEW: Multi-country comparison endpoint
     */
    @PostMapping("/compare")
    public ResponseEntity<?> compareCountries(@RequestBody CountryComparisonRequest request) {
        try {
            // Validate request
            if (request.getSourceCountries() == null || request.getSourceCountries().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("At least one source country must be provided");
            }
            
            if (request.getDestinationCountry() == null || request.getDestinationCountry().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("Destination country is required");
            }
            
            // Perform comparison
            CountryComparisonResult result = comparisonService.compareCountries(request);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error comparing countries: " + e.getMessage());
        }
    }
    
    /**
     * NEW: Get exchange rate between two currencies
     */
    @GetMapping("/exchange-rate")
    public ResponseEntity<?> getExchangeRate(
            @RequestParam String fromCurrency,
            @RequestParam String toCurrency) {
        try {
            BigDecimal rate = exchangeRateService.getExchangeRate(
                fromCurrency.toUpperCase(), 
                toCurrency.toUpperCase()
            );
            
            return ResponseEntity.ok(Map.of(
                "from", fromCurrency.toUpperCase(),
                "to", toCurrency.toUpperCase(),
                "rate", rate,
                "timestamp", java.time.LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error fetching exchange rate: " + e.getMessage());
        }
    }
    
    /**
     * NEW: Convert currency amount
     */
    @GetMapping("/convert")
    public ResponseEntity<?> convertCurrency(
            @RequestParam BigDecimal amount,
            @RequestParam String fromCurrency,
            @RequestParam String toCurrency) {
        try {
            BigDecimal converted = exchangeRateService.convertCurrency(
                amount,
                fromCurrency.toUpperCase(),
                toCurrency.toUpperCase()
            );
            
            return ResponseEntity.ok(Map.of(
                "originalAmount", amount,
                "fromCurrency", fromCurrency.toUpperCase(),
                "toCurrency", toCurrency.toUpperCase(),
                "convertedAmount", converted
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error converting currency: " + e.getMessage());
        }
    }
}
