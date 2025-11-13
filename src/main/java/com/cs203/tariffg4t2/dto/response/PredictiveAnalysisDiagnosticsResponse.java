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
public class PredictiveAnalysisDiagnosticsResponse {

    private boolean newsApiKeyPresent;
    private Boolean newsApiReachable;
    private String newsApiMessage;

    private boolean sentimentDataAvailable;
    private LocalDateTime latestSentimentTimestamp;
    private Double latestSentimentScore;
    private Integer sentimentArticlesAnalyzed;

    private boolean exchangeRateAvailable;
    private String exchangeRatePair;
    private LocalDate latestExchangeRateDate;
    private Double latestExchangeRate;

    private String currencyResolutionMessage;

    private List<String> warnings;
    private List<String> suggestions;

    private LocalDateTime generatedAt;
}

