package com.cs203.tariffg4t2.service.basic;

import com.cs203.tariffg4t2.dto.request.PredictiveAnalysisRequest;
import com.cs203.tariffg4t2.dto.response.PredictiveAnalysisResponse;
import com.cs203.tariffg4t2.model.basic.ExchangeRate;
import com.cs203.tariffg4t2.model.basic.NewsArticle;
import com.cs203.tariffg4t2.model.basic.SentimentAnalysis;
import com.cs203.tariffg4t2.repository.basic.ExchangeRateRepository;
import com.cs203.tariffg4t2.repository.basic.NewsArticleRepository;
import com.cs203.tariffg4t2.service.data.CurrencyCodeService;
import com.cs203.tariffg4t2.service.data.NewsAPIService;
import com.cs203.tariffg4t2.service.data.SentimentAnalysisService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PredictiveAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(PredictiveAnalysisService.class);
    
    private final NewsAPIService newsAPIService;
    private final SentimentAnalysisService sentimentAnalysisService;
    private final ExchangeRateService exchangeRateService;
    private final NewsArticleRepository newsArticleRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyCodeService currencyCodeService;
    
    /**
     * Main method: Analyze news sentiment and exchange rates to provide trading recommendation
     */
    public PredictiveAnalysisResponse analyzePrediction(PredictiveAnalysisRequest request) {
        logger.info("Starting predictive analysis for {} -> {}", 
                   request.getExportingCountry(), request.getImportingCountry());
        
        // Resolve currencies
        String importingCurrency = resolveCurrency(request.getImportingCountry());
        String exportingCurrency = resolveCurrency(request.getExportingCountry());
        
        if (importingCurrency == null || exportingCurrency == null) {
            throw new IllegalArgumentException("Could not resolve currencies for given countries");
        }
        
        // Step 1: Fetch and analyze news (if enabled)
        boolean liveNewsAvailable = false;
        String dataSource = "fallback_database";
        String message = "";
        
        if (Boolean.TRUE.equals(request.getEnableNewsAnalysis())) {
            try {
                // Fetch latest news
                List<NewsArticle> newArticles = newsAPIService.fetchTradeNews(7);  // Last 7 days
                
                // Analyze sentiment
                sentimentAnalysisService.processArticleSentiments(newArticles);
                
                // Calculate weekly aggregate
                LocalDate weekEnd = LocalDate.now();
                LocalDate weekStart = weekEnd.minusDays(6);
                sentimentAnalysisService.calculateWeeklySentiment(weekStart, weekEnd);
                
                liveNewsAvailable = true;
                dataSource = "live_api";
                message = "News sentiment updated from live News API";
                logger.info("Successfully fetched and analyzed {} news articles", newArticles.size());
                
            } catch (Exception e) {
                logger.warn("Failed to fetch live news, using database fallback: {}", e.getMessage());
                message = "Live News API unavailable. Using last stored sentiment data from database.";
                liveNewsAvailable = false;
            }
        }
        
        // Step 2: Get current sentiment
        SentimentAnalysis currentSentiment = sentimentAnalysisService.getLatestSentiment();
        
        if (currentSentiment == null) {
            throw new RuntimeException("No sentiment data available. Please enable news analysis first.");
        }
        
        // Step 3: Get exchange rate data
        ExchangeRate latestRate = exchangeRateRepository
            .findLatestByFromCurrencyAndToCurrency(exportingCurrency, importingCurrency)
            .orElse(null);
        
        if (latestRate == null) {
            throw new RuntimeException("No exchange rate data available for " + 
                                     exportingCurrency + " -> " + importingCurrency);
        }
        
        // Step 4: Get historical sentiment trend (past 4 weeks)
        List<SentimentAnalysis> sentimentHistory = sentimentAnalysisService.getSentimentHistory(4);
        
        // Step 5: Generate prediction using combined analysis
        PredictionResult prediction = generatePrediction(currentSentiment, latestRate, sentimentHistory);
        
        // Step 6: Get supporting news headlines
        List<NewsArticle> recentArticles = newsArticleRepository
            .findRecentArticles(LocalDateTime.now().minusDays(7));
        List<PredictiveAnalysisResponse.NewsHeadline> headlines = getSupportingHeadlines(recentArticles, 2);
        
        // Step 7: Build response
        return PredictiveAnalysisResponse.builder()
            .importingCountry(request.getImportingCountry())
            .exportingCountry(request.getExportingCountry())
            .recommendation(prediction.recommendation)
            .confidenceScore(prediction.confidence)
            .rationale(prediction.rationale)
            .currentSentiment(currentSentiment.getAverageSentiment())
            .sentimentTrend(currentSentiment.getTrend())
            .articlesAnalyzed(currentSentiment.getArticleCount())
            .currentExchangeRate(latestRate.getRate().doubleValue())
            .exchangeRateTrend(determineExchangeRateTrend(latestRate, exportingCurrency, importingCurrency))
            .supportingHeadlines(headlines)
            .sentimentHistory(convertToDataPoints(sentimentHistory))
            .liveNewsAvailable(liveNewsAvailable)
            .dataSource(dataSource)
            .message(message)
            .analysisTimestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Generate prediction based on sentiment and exchange rate trends
     */
    private PredictionResult generatePrediction(SentimentAnalysis sentiment, 
                                               ExchangeRate exchangeRate,
                                               List<SentimentAnalysis> history) {
        
        Double sentimentScore = sentiment.getAverageSentiment();
        String sentimentTrend = sentiment.getTrend();
        
        // Initialize scores
        double buyScore = 0.0;
        double waitScore = 0.0;
        double holdScore = 0.0;
        
        // Factor 1: Current sentiment (-1 to +1)
        if (sentimentScore > 0.3) {
            // Very positive sentiment → Good for buying
            buyScore += 0.4;
        } else if (sentimentScore < -0.3) {
            // Very negative sentiment → Wait for better conditions
            waitScore += 0.4;
        } else {
            // Neutral sentiment → Hold
            holdScore += 0.3;
        }
        
        // Factor 2: Sentiment trend
        if ("improving".equals(sentimentTrend)) {
            buyScore += 0.3;
        } else if ("declining".equals(sentimentTrend)) {
            waitScore += 0.3;
        } else {
            holdScore += 0.2;
        }
        
        // Factor 3: Sentiment stability (volatility)
        double volatility = calculateSentimentVolatility(history);
        if (volatility < 0.2) {
            // Low volatility → More confidence in current trend
            buyScore += 0.2;
            holdScore += 0.2;
        } else {
            // High volatility → Wait for stability
            waitScore += 0.3;
        }
        
        // Factor 4: Trend consistency
        boolean consistentPositive = history.stream()
            .allMatch(s -> s.getAverageSentiment() > 0.1);
        boolean consistentNegative = history.stream()
            .allMatch(s -> s.getAverageSentiment() < -0.1);
        
        if (consistentPositive) {
            buyScore += 0.1;
        } else if (consistentNegative) {
            waitScore += 0.1;
        }
        
        // Normalize scores to sum to 1.0
        double total = buyScore + waitScore + holdScore;
        if (total > 0) {
            buyScore /= total;
            waitScore /= total;
            holdScore /= total;
        }
        
        // Determine recommendation
        String recommendation;
        double confidence;
        String rationale;
        
        if (buyScore > waitScore && buyScore > holdScore) {
            recommendation = "BUY";
            confidence = buyScore;
            rationale = buildBuyRationale(sentimentScore, sentimentTrend, volatility);
        } else if (waitScore > buyScore && waitScore > holdScore) {
            recommendation = "WAIT";
            confidence = waitScore;
            rationale = buildWaitRationale(sentimentScore, sentimentTrend, volatility);
        } else {
            recommendation = "HOLD";
            confidence = holdScore;
            rationale = buildHoldRationale(sentimentScore, sentimentTrend, volatility);
        }
        
        logger.info("Prediction: {} (confidence: {:.2f})", recommendation, confidence);
        
        return new PredictionResult(recommendation, confidence, rationale);
    }
    
    /**
     * Calculate sentiment volatility (standard deviation)
     */
    private double calculateSentimentVolatility(List<SentimentAnalysis> history) {
        if (history.isEmpty()) return 0.0;
        
        double mean = history.stream()
            .mapToDouble(SentimentAnalysis::getAverageSentiment)
            .average()
            .orElse(0.0);
        
        double variance = history.stream()
            .mapToDouble(s -> Math.pow(s.getAverageSentiment() - mean, 2))
            .average()
            .orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
    /**
     * Build rationale for BUY recommendation
     */
    private String buildBuyRationale(double sentiment, String trend, double volatility) {
        return String.format(
            "Market sentiment is positive (%.2f) and %s. " +
            "Trade news indicates favorable conditions with %s volatility. " +
            "This is a good time to proceed with purchases.",
            sentiment, trend, volatility < 0.2 ? "low" : "moderate"
        );
    }
    
    /**
     * Build rationale for WAIT recommendation
     */
    private String buildWaitRationale(double sentiment, String trend, double volatility) {
        return String.format(
            "Market sentiment is currently %s (%.2f) and %s. " +
            "%s suggests waiting for more favorable conditions before making large purchases.",
            sentiment < 0 ? "negative" : "uncertain", 
            sentiment, 
            trend,
            volatility > 0.3 ? "High market volatility" : "Current trends"
        );
    }
    
    /**
     * Build rationale for HOLD recommendation
     */
    private String buildHoldRationale(double sentiment, String trend, double volatility) {
        return String.format(
            "Market sentiment is neutral (%.2f) with %s trend. " +
            "Consider maintaining current positions and monitoring for clearer signals.",
            sentiment, trend
        );
    }
    
    /**
     * Determine exchange rate trend
     */
    private String determineExchangeRateTrend(ExchangeRate current, String fromCurrency, String toCurrency) {
        LocalDate twoWeeksAgo = LocalDate.now().minusWeeks(2);
        
        List<ExchangeRate> recentRates = exchangeRateRepository
            .findByFromCurrencyAndToCurrencyAndRateDateBetween(
                fromCurrency, toCurrency, twoWeeksAgo, LocalDate.now()
            );
        
        if (recentRates.size() < 2) return "stable";
        
        double firstAvg = recentRates.subList(0, recentRates.size() / 2).stream()
            .mapToDouble(r -> r.getRate().doubleValue())
            .average()
            .orElse(0.0);
        
        double secondAvg = recentRates.subList(recentRates.size() / 2, recentRates.size()).stream()
            .mapToDouble(r -> r.getRate().doubleValue())
            .average()
            .orElse(0.0);
        
        double change = ((secondAvg - firstAvg) / firstAvg) * 100;
        
        if (change > 1.0) return "increasing";
        if (change < -1.0) return "decreasing";
        return "stable";
    }
    
    /**
     * Get supporting news headlines
     */
    private List<PredictiveAnalysisResponse.NewsHeadline> getSupportingHeadlines(
            List<NewsArticle> articles, int count) {
        
        return articles.stream()
            .sorted(Comparator.comparing(NewsArticle::getPublishedAt).reversed())
            .limit(count)
            .map(article -> PredictiveAnalysisResponse.NewsHeadline.builder()
                .title(article.getTitle())
                .source(article.getSource())
                .publishedAt(article.getPublishedAt())
                .sentimentScore(article.getSentimentScore())
                .url(article.getUrl())
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * Convert sentiment history to data points
     */
    private List<PredictiveAnalysisResponse.SentimentDataPoint> convertToDataPoints(
            List<SentimentAnalysis> history) {
        
        return history.stream()
            .map(s -> PredictiveAnalysisResponse.SentimentDataPoint.builder()
                .weekStart(s.getWeekStartDate())
                .weekEnd(s.getWeekEndDate())
                .averageSentiment(s.getAverageSentiment())
                .articleCount(s.getArticleCount())
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * Resolve currency from country code
     */
    private String resolveCurrency(String country) {
        // Try direct currency code lookup
        String currency = currencyCodeService.getCurrencyCode(country.toUpperCase());
        if (currency != null) {
            return currency;
        }
        
        // Additional resolution logic here if needed
        return null;
    }
    
    /**
     * DEBUG: Manually fetch news to test API connection
     */
    public List<NewsArticle> debugFetchNews() throws Exception {
        logger.info("DEBUG: Attempting to fetch news from API...");
        
        // Fetch news
        List<NewsArticle> articles = newsAPIService.fetchTradeNews(7);
        logger.info("DEBUG: Fetched {} articles from API", articles.size());
        
        if (articles.isEmpty()) {
            logger.warn("DEBUG: No articles returned from API");
            throw new Exception("News API returned 0 articles. Check API key and query parameters.");
        }
        
        // Process sentiment
        sentimentAnalysisService.processArticleSentiments(articles);
        logger.info("DEBUG: Processed and saved {} articles with sentiment scores", articles.size());
        
        // Calculate weekly sentiment (flush to ensure data is committed)
        LocalDate weekEnd = LocalDate.now();
        LocalDate weekStart = weekEnd.minusDays(6);
        
        SentimentAnalysis analysis = sentimentAnalysisService.calculateWeeklySentiment(weekStart, weekEnd);
        
        if (analysis != null) {
            logger.info("DEBUG: Created weekly analysis: avg={}, articles={}", 
                       analysis.getAverageSentiment(), analysis.getArticleCount());
        } else {
            logger.warn("DEBUG: Weekly analysis returned null - might need to wait for transaction commit");
            // This is OK - the articles are saved, just return them
        }
        
        return articles;
    }
    
    /**
     * Inner class to hold prediction result
     */
    private static class PredictionResult {
        String recommendation;
        double confidence;
        String rationale;
        
        PredictionResult(String recommendation, double confidence, String rationale) {
            this.recommendation = recommendation;
            this.confidence = confidence;
            this.rationale = rationale;
        }
    }
}

