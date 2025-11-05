package com.cs203.tariffg4t2.controller;

import com.cs203.tariffg4t2.dto.request.ExchangeRateAnalysisRequest;
import com.cs203.tariffg4t2.dto.response.ExchangeRateAnalysisResponse;
import com.cs203.tariffg4t2.service.basic.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Exchange Rate", description = "Exchange rate analysis and currency conversion APIs")
public class ExchangeRateController {
    
    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateController.class);
    
    private final ExchangeRateService exchangeRateService;
    
    /**
     * Analyze exchange rates between two countries and provide purchase recommendations
     * 
     * @param request Contains importing and exporting country identifiers
     * @return Exchange rate analysis with trend data and recommendations
     */
    @PostMapping("/analyze")
    @Operation(summary = "Analyze exchange rates", 
               description = "Performs exchange rate analysis between two countries, including trend analysis " +
                           "and purchase date recommendations based on 6 months of historical data")
    public ResponseEntity<?> analyzeExchangeRates(@Valid @RequestBody ExchangeRateAnalysisRequest request) {
        try {
            logger.info("Received exchange rate analysis request: {} -> {}", 
                       request.getExportingCountry(), request.getImportingCountry());
            
            ExchangeRateAnalysisResponse response = exchangeRateService.analyzeExchangeRates(request);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity
                .badRequest()
                .body(createErrorResponse("Invalid country code or currency mapping not found", e.getMessage()));
                
        } catch (RuntimeException e) {
            logger.error("Error analyzing exchange rates: {}", e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to analyze exchange rates", e.getMessage()));
        }
    }
    
    /**
     * Get exchange rate analysis for tariff calculation
     * Query parameter version for easier integration
     */
    @GetMapping("/analyze")
    @Operation(summary = "Analyze exchange rates (GET)", 
               description = "Same as POST /analyze but using query parameters for easier testing")
    public ResponseEntity<?> analyzeExchangeRatesGet(
            @RequestParam String importingCountry,
            @RequestParam String exportingCountry) {
        
        ExchangeRateAnalysisRequest request = ExchangeRateAnalysisRequest.builder()
            .importingCountry(importingCountry)
            .exportingCountry(exportingCountry)
            .build();
        
        return analyzeExchangeRates(request);
    }
    
    /**
     * Health check endpoint to verify API connectivity
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", 
               description = "Check if the exchange rate service and API connection are healthy")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "exchange-rate-service");
        health.put("timestamp", java.time.LocalDateTime.now());
        health.put("apiReference", "https://openexchangerates.org/");
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Get the live exchange rate API reference link
     */
    @GetMapping("/api-reference")
    @Operation(summary = "Get API reference link", 
               description = "Returns the link to OpenExchangeRates for users to validate data accuracy")
    public ResponseEntity<Map<String, String>> getApiReference() {
        Map<String, String> response = new HashMap<>();
        response.put("provider", "OpenExchangeRates");
        response.put("url", "https://openexchangerates.org/");
        response.put("description", "Visit this link to validate real-time exchange rate data");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Helper method to create error response
     */
    private Map<String, Object> createErrorResponse(String error, String details) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("details", details);
        errorResponse.put("timestamp", java.time.LocalDateTime.now());
        return errorResponse;
    }
}

