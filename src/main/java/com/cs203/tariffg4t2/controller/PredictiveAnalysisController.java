package com.cs203.tariffg4t2.controller;

import com.cs203.tariffg4t2.dto.request.PredictiveAnalysisRequest;
import com.cs203.tariffg4t2.dto.response.PredictiveAnalysisResponse;
import com.cs203.tariffg4t2.service.basic.PredictiveAnalysisService;
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
@RequestMapping("/api/predictive-analysis")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Predictive Analysis", description = "AI-driven predictions based on news sentiment and market trends")
public class PredictiveAnalysisController {
    
    private static final Logger logger = LoggerFactory.getLogger(PredictiveAnalysisController.class);
    
    private final PredictiveAnalysisService predictiveAnalysisService;
    
    /**
     * Get AI-driven prediction for optimal purchase timing
     * 
     * @param request Contains importing/exporting countries and analysis preferences
     * @return Prediction with BUY/HOLD/WAIT recommendation and supporting data
     */
    @PostMapping("/predict")
    @Operation(summary = "Get purchase prediction", 
               description = "Analyzes news sentiment and exchange rates to provide BUY/HOLD/WAIT recommendation with confidence score")
    public ResponseEntity<?> getPrediction(@Valid @RequestBody PredictiveAnalysisRequest request) {
        try {
            logger.info("Received prediction request: {} -> {} (news analysis: {})", 
                       request.getExportingCountry(), 
                       request.getImportingCountry(),
                       request.getEnableNewsAnalysis());
            
            PredictiveAnalysisResponse response = predictiveAnalysisService.analyzePrediction(request);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity
                .badRequest()
                .body(createErrorResponse("Invalid parameters", e.getMessage()));
                
        } catch (RuntimeException e) {
            logger.error("Error generating prediction: {}", e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Prediction failed", e.getMessage()));
        }
    }
    
    /**
     * GET version for easier testing
     */
    @GetMapping("/predict")
    @Operation(summary = "Get purchase prediction (GET)", 
               description = "Same as POST but using query parameters")
    public ResponseEntity<?> getPredictionGet(
            @RequestParam String importingCountry,
            @RequestParam String exportingCountry,
            @RequestParam(defaultValue = "true") Boolean enableNewsAnalysis) {
        
        PredictiveAnalysisRequest request = PredictiveAnalysisRequest.builder()
            .importingCountry(importingCountry)
            .exportingCountry(exportingCountry)
            .enableNewsAnalysis(enableNewsAnalysis)
            .build();
        
        return getPrediction(request);
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", 
               description = "Check if the predictive analysis service is operational")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "predictive-analysis-service");
        health.put("timestamp", java.time.LocalDateTime.now());
        health.put("newsApiReference", "https://newsapi.org/");
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Get News API reference
     */
    @GetMapping("/news-api-reference")
    @Operation(summary = "Get News API reference", 
               description = "Returns link to News API for validation")
    public ResponseEntity<Map<String, String>> getNewsAPIReference() {
        Map<String, String> response = new HashMap<>();
        response.put("provider", "NewsAPI.org");
        response.put("url", "https://newsapi.org/");
        response.put("description", "Visit this link to sign up and validate news data sources");
        
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

