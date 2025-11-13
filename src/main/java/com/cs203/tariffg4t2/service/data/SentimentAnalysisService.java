package com.cs203.tariffg4t2.service.data;

import com.cs203.tariffg4t2.model.basic.NewsArticle;
import com.cs203.tariffg4t2.model.basic.SentimentAnalysis;
import com.cs203.tariffg4t2.repository.basic.NewsArticleRepository;
import com.cs203.tariffg4t2.repository.basic.SentimentAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class SentimentAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(SentimentAnalysisService.class);
    
    // Sentiment keywords and their weights
    private static final Map<String, Double> POSITIVE_KEYWORDS = new HashMap<>();
    private static final Map<String, Double> NEGATIVE_KEYWORDS = new HashMap<>();
    
    static {
        // Positive sentiment keywords
        POSITIVE_KEYWORDS.put("agreement", 0.5);
        POSITIVE_KEYWORDS.put("boost", 0.6);
        POSITIVE_KEYWORDS.put("growth", 0.7);
        POSITIVE_KEYWORDS.put("increase", 0.4);
        POSITIVE_KEYWORDS.put("expansion", 0.6);
        POSITIVE_KEYWORDS.put("strengthen", 0.5);
        POSITIVE_KEYWORDS.put("improve", 0.6);
        POSITIVE_KEYWORDS.put("positive", 0.5);
        POSITIVE_KEYWORDS.put("cooperation", 0.5);
        POSITIVE_KEYWORDS.put("partnership", 0.5);
        POSITIVE_KEYWORDS.put("deal", 0.4);
        POSITIVE_KEYWORDS.put("opportunity", 0.6);
        POSITIVE_KEYWORDS.put("recovery", 0.7);
        
        // Negative sentiment keywords
        NEGATIVE_KEYWORDS.put("tariff", -0.4);
        NEGATIVE_KEYWORDS.put("war", -0.8);
        NEGATIVE_KEYWORDS.put("dispute", -0.6);
        NEGATIVE_KEYWORDS.put("conflict", -0.7);
        NEGATIVE_KEYWORDS.put("decline", -0.6);
        NEGATIVE_KEYWORDS.put("fall", -0.5);
        NEGATIVE_KEYWORDS.put("drop", -0.5);
        NEGATIVE_KEYWORDS.put("threat", -0.7);
        NEGATIVE_KEYWORDS.put("tension", -0.6);
        NEGATIVE_KEYWORDS.put("sanction", -0.7);
        NEGATIVE_KEYWORDS.put("crisis", -0.8);
        NEGATIVE_KEYWORDS.put("uncertainty", -0.5);
        NEGATIVE_KEYWORDS.put("risk", -0.4);
    }
    
    private final NewsArticleRepository newsArticleRepository;
    private final SentimentAnalysisRepository sentimentAnalysisRepository;
    
    /**
     * Analyze sentiment of a single article using keyword-based approach
     * Returns score between -1.0 (very negative) and +1.0 (very positive)
     */
    public Double analyzeSentiment(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        
        String lowerText = text.toLowerCase();
        double score = 0.0;
        int matchCount = 0;
        
        // Check for positive keywords
        for (Map.Entry<String, Double> entry : POSITIVE_KEYWORDS.entrySet()) {
            if (lowerText.contains(entry.getKey())) {
                score += entry.getValue();
                matchCount++;
            }
        }
        
        // Check for negative keywords
        for (Map.Entry<String, Double> entry : NEGATIVE_KEYWORDS.entrySet()) {
            if (lowerText.contains(entry.getKey())) {
                score += entry.getValue();  // Already negative
                matchCount++;
            }
        }
        
        // Normalize score
        if (matchCount > 0) {
            score = score / matchCount;
        }
        
        // Clamp between -1 and 1
        score = Math.max(-1.0, Math.min(1.0, score));
        
        return score;
    }
    
    /**
     * Process and store sentiment for all articles
     */
    public void processArticleSentiments(List<NewsArticle> articles) {
        logger.info("Processing sentiment for {} articles", articles.size());
        
        for (NewsArticle article : articles) {
            String content = (article.getTitle() != null ? article.getTitle() : "") + " " +
                           (article.getDescription() != null ? article.getDescription() : "");
            
            Double sentiment = analyzeSentiment(content);
            article.setSentimentScore(sentiment);
            
            logger.debug("Article '{}' sentiment: {}", 
                        article.getTitle() != null ? article.getTitle().substring(0, Math.min(50, article.getTitle().length())) : "N/A", 
                        sentiment);
        }
        
        // Save all articles with sentiment scores
        newsArticleRepository.saveAll(articles);
        logger.info("Saved {} articles with sentiment scores", articles.size());
    }
    
    /**
     * Calculate and store weekly sentiment aggregate
     */
    public SentimentAnalysis calculateWeeklySentiment(LocalDate weekStart, LocalDate weekEnd) {
        logger.info("Calculating weekly sentiment: {} to {}", weekStart, weekEnd);
        
        LocalDateTime startDateTime = weekStart.atStartOfDay();
        LocalDateTime endDateTime = weekEnd.atTime(23, 59, 59);
        
        // Get average sentiment from database
        Double avgSentiment = newsArticleRepository.getAverageSentiment(startDateTime, endDateTime);
        
        if (avgSentiment == null) {
            logger.warn("No sentiment data available for week {} to {}", weekStart, weekEnd);
            return null;
        }
        
        // Count articles by sentiment polarity
        Long positiveCount = newsArticleRepository.countPositiveArticles(startDateTime, endDateTime);
        Long negativeCount = newsArticleRepository.countNegativeArticles(startDateTime, endDateTime);
        Long neutralCount = newsArticleRepository.countNeutralArticles(startDateTime, endDateTime);
        
        Long totalCount = positiveCount + negativeCount + neutralCount;
        
        // Determine trend by comparing with previous week
        String trend = determineTrend(weekStart, avgSentiment);
        
        // Create or update sentiment analysis record
        SentimentAnalysis analysis = sentimentAnalysisRepository
            .findByWeekStartDateAndWeekEndDate(weekStart, weekEnd)
            .orElse(new SentimentAnalysis());
        
        analysis.setWeekStartDate(weekStart);
        analysis.setWeekEndDate(weekEnd);
        analysis.setAverageSentiment(avgSentiment);
        analysis.setArticleCount(totalCount.intValue());
        analysis.setPositiveCount(positiveCount.intValue());
        analysis.setNegativeCount(negativeCount.intValue());
        analysis.setNeutralCount(neutralCount.intValue());
        analysis.setTrend(trend);
        
        analysis = sentimentAnalysisRepository.save(analysis);
        logger.info("Saved weekly sentiment: avg={}, trend={}, articles={}", 
                   avgSentiment, trend, totalCount);
        
        return analysis;
    }
    
    /**
     * Determine sentiment trend by comparing with previous week
     */
    private String determineTrend(LocalDate currentWeekStart, Double currentSentiment) {
        LocalDate previousWeekStart = currentWeekStart.minusWeeks(1);
        LocalDate previousWeekEnd = previousWeekStart.plusDays(6);
        
        SentimentAnalysis previousWeek = sentimentAnalysisRepository
            .findByWeekStartDateAndWeekEndDate(previousWeekStart, previousWeekEnd)
            .orElse(null);
        
        if (previousWeek == null) {
            return "stable";  // No previous data to compare
        }
        
        Double previousSentiment = previousWeek.getAverageSentiment();
        Double change = currentSentiment - previousSentiment;
        
        // Threshold: 0.1 change is significant
        if (change > 0.1) {
            return "improving";
        } else if (change < -0.1) {
            return "declining";
        } else {
            return "stable";
        }
    }
    
    /**
     * Get sentiment history for the past N weeks
     */
    public List<SentimentAnalysis> getSentimentHistory(int weeks) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusWeeks(weeks);
        
        return sentimentAnalysisRepository.findByDateRange(startDate, endDate);
    }
    
    /**
     * Get most recent sentiment analysis
     */
    public SentimentAnalysis getLatestSentiment() {
        return sentimentAnalysisRepository.findLatest().orElse(null);
    }
}

