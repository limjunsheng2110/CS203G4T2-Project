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
public class ExchangeRateAnalysisRequest {
    
    @NotBlank(message = "Importing country is required")
    private String importingCountry;  // Can be country code (alpha-2, alpha-3) or full name
    
    @NotBlank(message = "Exporting country is required")
    private String exportingCountry;  // Can be country code (alpha-2, alpha-3) or full name
}

