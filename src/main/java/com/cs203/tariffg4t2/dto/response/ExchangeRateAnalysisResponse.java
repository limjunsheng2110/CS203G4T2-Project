package com.cs203.tariffg4t2.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRateAnalysisResponse {
    
    // Basic exchange rate info
    private String importingCountry;
    private String exportingCountry;
    private String importingCurrency;
    private String exportingCurrency;
    
    // Current exchange rate
    private BigDecimal currentRate;
    private LocalDate currentRateDate;
    
    // Trend analysis (past 6 months)
    private BigDecimal averageRate;
    private BigDecimal minRate;
    private LocalDate minRateDate;
    private BigDecimal maxRate;
    private LocalDate maxRateDate;
    
    // Recommendation
    private LocalDate recommendedPurchaseDate;
    private String recommendation;  // Explanation of the recommendation
    private String trendAnalysis;   // "increasing", "decreasing", "stable"
    
    // Historical data points for charting
    private List<ExchangeRateDataPoint> historicalRates;
    
    // API status
    private boolean liveDataAvailable;
    private String dataSource;  // "live_api" or "fallback_database"
    private String message;     // Any additional messages
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExchangeRateDataPoint {
        private LocalDate date;
        private BigDecimal rate;
    }
}

