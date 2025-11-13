package com.cs203.tariffg4t2.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictiveAnalysisRequest {
    
    @NotBlank(message = "Importing country is required")
    private String importingCountry;
    
    @NotBlank(message = "Exporting country is required")
    private String exportingCountry;
    
    private Boolean enableNewsAnalysis;  // Toggle for news-based predictions
}

