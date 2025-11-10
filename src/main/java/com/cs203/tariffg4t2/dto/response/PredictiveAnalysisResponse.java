package com.cs203.tariffg4t2.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictiveAnalysisResponse {
    
    // Basic Info
    private String importingCountry;
    private String exportingCountry;
    
    // Prediction Result
    private String recommendation;  // "BUY", "HOLD", "WAIT"
    private Double confidenceScore;  // 0.0 to 1.0 (0% to 100%)
    private String rationale;  // Explanation of the recommendation
    
    // Sentiment Data
    private Double currentSentiment;  // Latest weekly sentiment (-1 to +1)
    private String sentimentTrend;  // "improving", "declining", "stable"
    private Integer articlesAnalyzed;  // Number of news articles
    
    // Exchange Rate Context
    private Double currentExchangeRate;
    private String exchangeRateTrend;  // From previous exchange rate analysis
    
    // Supporting Evidence
    private List<NewsHeadline> supportingHeadlines;  // 2 sample headlines
    
    // Historical Sentiment Trend (for visualization)
    private List<SentimentDataPoint> sentimentHistory;
    
    // Data Source Status
    private boolean liveNewsAvailable;
    private String dataSource;  // "live_api" or "fallback_database"
    private String message;  // Status or fallback message
    private LocalDateTime analysisTimestamp;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NewsHeadline {
        private String title;
        private String source;
        private LocalDateTime publishedAt;
        private Double sentimentScore;
        private String url;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SentimentDataPoint {
        private LocalDate weekStart;
        private LocalDate weekEnd;
        private Double averageSentiment;
        private Integer articleCount;
    }
}

