package com.cs203.tariffg4t2.service.data;

import com.cs203.tariffg4t2.model.basic.NewsArticle;
import com.cs203.tariffg4t2.model.basic.SentimentAnalysis;
import com.cs203.tariffg4t2.repository.basic.NewsArticleRepository;
import com.cs203.tariffg4t2.repository.basic.SentimentAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SentimentAnalysisServiceTest {

    @Mock
    private NewsArticleRepository newsArticleRepository;

    @Mock
    private SentimentAnalysisRepository sentimentAnalysisRepository;

    @InjectMocks
    private SentimentAnalysisService sentimentAnalysisService;

    @Test
    void testAnalyzeSentiment_NullText() {
        Double sentiment = sentimentAnalysisService.analyzeSentiment(null);
        assertEquals(0.0, sentiment);
    }

    @Test
    void testAnalyzeSentiment_EmptyText() {
        Double sentiment = sentimentAnalysisService.analyzeSentiment("");
        assertEquals(0.0, sentiment);
    }

    @Test
    void testAnalyzeSentiment_PositiveKeywords() {
        String text = "Trade agreement brings growth and opportunity for expansion and boost in cooperation";
        Double sentiment = sentimentAnalysisService.analyzeSentiment(text);
        assertTrue(sentiment > 0, "Sentiment should be positive");
    }

    @Test
    void testAnalyzeSentiment_NegativeKeywords() {
        String text = "Trade war escalates with new tariffs causing crisis and uncertainty amid conflict";
        Double sentiment = sentimentAnalysisService.analyzeSentiment(text);
        assertTrue(sentiment < 0, "Sentiment should be negative");
    }

    @Test
    void testAnalyzeSentiment_MixedKeywords() {
        String text = "Trade agreement reached despite tariff concerns and uncertainty about deal";
        Double sentiment = sentimentAnalysisService.analyzeSentiment(text);
        assertNotNull(sentiment);
        assertTrue(sentiment >= -1.0 && sentiment <= 1.0);
    }

    @Test
    void testAnalyzeSentiment_NoKeywords() {
        String text = "The weather is nice today";
        Double sentiment = sentimentAnalysisService.analyzeSentiment(text);
        assertEquals(0.0, sentiment);
    }

    @Test
    void testAnalyzeSentiment_CaseInsensitive() {
        String text = "GROWTH and EXPANSION with AGREEMENT";
        Double sentiment = sentimentAnalysisService.analyzeSentiment(text);
        assertTrue(sentiment > 0, "Sentiment should be positive regardless of case");
    }

    @Test
    void testAnalyzeSentiment_ClampedToOne() {
        String text = "growth boost expansion improve strengthen recovery opportunity partnership cooperation positive agreement deal";
        Double sentiment = sentimentAnalysisService.analyzeSentiment(text);
        assertTrue(sentiment <= 1.0, "Sentiment should be clamped to maximum 1.0");
    }

    @Test
    void testAnalyzeSentiment_ClampedToNegativeOne() {
        String text = "war crisis conflict threat sanction decline risk uncertainty tariff tension dispute fall drop";
        Double sentiment = sentimentAnalysisService.analyzeSentiment(text);
        assertTrue(sentiment >= -1.0, "Sentiment should be clamped to minimum -1.0");
    }

    @Test
    void testProcessArticleSentiments_EmptyList() {
        List<NewsArticle> articles = new ArrayList<>();

        sentimentAnalysisService.processArticleSentiments(articles);

        verify(newsArticleRepository, times(1)).saveAll(articles);
    }

    @Test
    void testProcessArticleSentiments_SingleArticle() {
        NewsArticle article = new NewsArticle();
        article.setTitle("Trade agreement boosts economy");
        article.setDescription("Positive growth expected");

        List<NewsArticle> articles = List.of(article);

        sentimentAnalysisService.processArticleSentiments(articles);

        assertNotNull(article.getSentimentScore());
        assertTrue(article.getSentimentScore() > 0);
        verify(newsArticleRepository, times(1)).saveAll(articles);
    }

    @Test
    void testProcessArticleSentiments_MultipleArticles() {
        NewsArticle article1 = new NewsArticle();
        article1.setTitle("Trade war escalates");
        article1.setDescription("Tariff concerns rise");

        NewsArticle article2 = new NewsArticle();
        article2.setTitle("Economic growth improves");
        article2.setDescription("Positive outlook");

        List<NewsArticle> articles = List.of(article1, article2);

        sentimentAnalysisService.processArticleSentiments(articles);

        assertNotNull(article1.getSentimentScore());
        assertNotNull(article2.getSentimentScore());
        assertTrue(article1.getSentimentScore() < article2.getSentimentScore());
        verify(newsArticleRepository, times(1)).saveAll(articles);
    }

    @Test
    void testProcessArticleSentiments_NullTitleAndDescription() {
        NewsArticle article = new NewsArticle();
        article.setTitle(null);
        article.setDescription(null);

        List<NewsArticle> articles = List.of(article);

        sentimentAnalysisService.processArticleSentiments(articles);

        assertNotNull(article.getSentimentScore());
        assertEquals(0.0, article.getSentimentScore());
        verify(newsArticleRepository, times(1)).saveAll(articles);
    }

    @Test
    void testCalculateWeeklySentiment_Success() {
        LocalDate weekStart = LocalDate.of(2025, 11, 10);
        LocalDate weekEnd = LocalDate.of(2025, 11, 16);

        when(newsArticleRepository.getAverageSentiment(any(), any())).thenReturn(0.5);
        when(newsArticleRepository.countPositiveArticles(any(), any())).thenReturn(10L);
        when(newsArticleRepository.countNegativeArticles(any(), any())).thenReturn(3L);
        when(newsArticleRepository.countNeutralArticles(any(), any())).thenReturn(2L);
        when(sentimentAnalysisRepository.findByWeekStartDateAndWeekEndDate(any(), any()))
            .thenReturn(Optional.empty());

        SentimentAnalysis mockAnalysis = new SentimentAnalysis();
        mockAnalysis.setId(1L);
        mockAnalysis.setWeekStartDate(weekStart);
        mockAnalysis.setWeekEndDate(weekEnd);
        mockAnalysis.setAverageSentiment(0.5);
        mockAnalysis.setArticleCount(15);

        when(sentimentAnalysisRepository.save(any(SentimentAnalysis.class))).thenReturn(mockAnalysis);

        SentimentAnalysis result = sentimentAnalysisService.calculateWeeklySentiment(weekStart, weekEnd);

        assertNotNull(result);
        assertEquals(0.5, result.getAverageSentiment());
        assertEquals(15, result.getArticleCount());
        verify(sentimentAnalysisRepository, times(1)).save(any(SentimentAnalysis.class));
    }

    @Test
    void testCalculateWeeklySentiment_NoData() {
        LocalDate weekStart = LocalDate.of(2025, 11, 10);
        LocalDate weekEnd = LocalDate.of(2025, 11, 16);

        when(newsArticleRepository.getAverageSentiment(any(), any())).thenReturn(null);

        SentimentAnalysis result = sentimentAnalysisService.calculateWeeklySentiment(weekStart, weekEnd);

        assertNull(result);
        verify(sentimentAnalysisRepository, never()).save(any());
    }

    @Test
    void testCalculateWeeklySentiment_UpdateExisting() {
        LocalDate weekStart = LocalDate.of(2025, 11, 10);
        LocalDate weekEnd = LocalDate.of(2025, 11, 16);
        LocalDate previousWeekStart = LocalDate.of(2025, 11, 3);
        LocalDate previousWeekEnd = LocalDate.of(2025, 11, 9);

        SentimentAnalysis existing = new SentimentAnalysis();
        existing.setId(1L);
        existing.setWeekStartDate(weekStart);
        existing.setWeekEndDate(weekEnd);

        // Mock previous week data for trend calculation
        SentimentAnalysis previousWeek = new SentimentAnalysis();
        previousWeek.setAverageSentiment(0.25);

        when(newsArticleRepository.getAverageSentiment(any(), any())).thenReturn(0.3);
        when(newsArticleRepository.countPositiveArticles(any(), any())).thenReturn(5L);
        when(newsArticleRepository.countNegativeArticles(any(), any())).thenReturn(5L);
        when(newsArticleRepository.countNeutralArticles(any(), any())).thenReturn(5L);

        // First call returns the existing analysis for current week, second call returns previous week for trend
        when(sentimentAnalysisRepository.findByWeekStartDateAndWeekEndDate(weekStart, weekEnd))
            .thenReturn(Optional.of(existing));
        when(sentimentAnalysisRepository.findByWeekStartDateAndWeekEndDate(previousWeekStart, previousWeekEnd))
            .thenReturn(Optional.of(previousWeek));

        when(sentimentAnalysisRepository.save(any(SentimentAnalysis.class))).thenReturn(existing);

        SentimentAnalysis result = sentimentAnalysisService.calculateWeeklySentiment(weekStart, weekEnd);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(sentimentAnalysisRepository, times(1)).save(existing);
    }

    @Test
    void testCalculateWeeklySentiment_TrendImproving() {
        LocalDate weekStart = LocalDate.of(2025, 11, 10);
        LocalDate weekEnd = LocalDate.of(2025, 11, 16);
        LocalDate previousWeekStart = LocalDate.of(2025, 11, 3);
        LocalDate previousWeekEnd = LocalDate.of(2025, 11, 9);

        // Previous week data
        SentimentAnalysis previousWeek = new SentimentAnalysis();
        previousWeek.setAverageSentiment(0.2);

        when(newsArticleRepository.getAverageSentiment(any(), any())).thenReturn(0.5); // Improved
        when(newsArticleRepository.countPositiveArticles(any(), any())).thenReturn(10L);
        when(newsArticleRepository.countNegativeArticles(any(), any())).thenReturn(2L);
        when(newsArticleRepository.countNeutralArticles(any(), any())).thenReturn(3L);
        when(sentimentAnalysisRepository.findByWeekStartDateAndWeekEndDate(weekStart, weekEnd))
            .thenReturn(Optional.empty());
        when(sentimentAnalysisRepository.findByWeekStartDateAndWeekEndDate(previousWeekStart, previousWeekEnd))
            .thenReturn(Optional.of(previousWeek));

        SentimentAnalysis mockAnalysis = new SentimentAnalysis();
        mockAnalysis.setTrend("improving");
        when(sentimentAnalysisRepository.save(any(SentimentAnalysis.class))).thenReturn(mockAnalysis);

        SentimentAnalysis result = sentimentAnalysisService.calculateWeeklySentiment(weekStart, weekEnd);

        assertNotNull(result);
        assertEquals("improving", result.getTrend());
    }

    @Test
    void testCalculateWeeklySentiment_TrendDeclining() {
        LocalDate weekStart = LocalDate.of(2025, 11, 10);
        LocalDate weekEnd = LocalDate.of(2025, 11, 16);
        LocalDate previousWeekStart = LocalDate.of(2025, 11, 3);
        LocalDate previousWeekEnd = LocalDate.of(2025, 11, 9);

        SentimentAnalysis previousWeek = new SentimentAnalysis();
        previousWeek.setAverageSentiment(0.5);

        when(newsArticleRepository.getAverageSentiment(any(), any())).thenReturn(0.2); // Declined
        when(newsArticleRepository.countPositiveArticles(any(), any())).thenReturn(2L);
        when(newsArticleRepository.countNegativeArticles(any(), any())).thenReturn(10L);
        when(newsArticleRepository.countNeutralArticles(any(), any())).thenReturn(3L);
        when(sentimentAnalysisRepository.findByWeekStartDateAndWeekEndDate(weekStart, weekEnd))
            .thenReturn(Optional.empty());
        when(sentimentAnalysisRepository.findByWeekStartDateAndWeekEndDate(previousWeekStart, previousWeekEnd))
            .thenReturn(Optional.of(previousWeek));

        SentimentAnalysis mockAnalysis = new SentimentAnalysis();
        mockAnalysis.setTrend("declining");
        when(sentimentAnalysisRepository.save(any(SentimentAnalysis.class))).thenReturn(mockAnalysis);

        SentimentAnalysis result = sentimentAnalysisService.calculateWeeklySentiment(weekStart, weekEnd);

        assertNotNull(result);
        assertEquals("declining", result.getTrend());
    }

    @Test
    void testCalculateWeeklySentiment_TrendStable() {
        LocalDate weekStart = LocalDate.of(2025, 11, 10);
        LocalDate weekEnd = LocalDate.of(2025, 11, 16);
        LocalDate previousWeekStart = LocalDate.of(2025, 11, 3);
        LocalDate previousWeekEnd = LocalDate.of(2025, 11, 9);

        SentimentAnalysis previousWeek = new SentimentAnalysis();
        previousWeek.setAverageSentiment(0.3);

        when(newsArticleRepository.getAverageSentiment(any(), any())).thenReturn(0.35); // Small change
        when(newsArticleRepository.countPositiveArticles(any(), any())).thenReturn(5L);
        when(newsArticleRepository.countNegativeArticles(any(), any())).thenReturn(5L);
        when(newsArticleRepository.countNeutralArticles(any(), any())).thenReturn(5L);
        when(sentimentAnalysisRepository.findByWeekStartDateAndWeekEndDate(weekStart, weekEnd))
            .thenReturn(Optional.empty());
        when(sentimentAnalysisRepository.findByWeekStartDateAndWeekEndDate(previousWeekStart, previousWeekEnd))
            .thenReturn(Optional.of(previousWeek));

        SentimentAnalysis mockAnalysis = new SentimentAnalysis();
        mockAnalysis.setTrend("stable");
        when(sentimentAnalysisRepository.save(any(SentimentAnalysis.class))).thenReturn(mockAnalysis);

        SentimentAnalysis result = sentimentAnalysisService.calculateWeeklySentiment(weekStart, weekEnd);

        assertNotNull(result);
        assertEquals("stable", result.getTrend());
    }

    @Test
    void testCalculateWeeklySentiment_NoPreviousWeek() {
        LocalDate weekStart = LocalDate.of(2025, 11, 10);
        LocalDate weekEnd = LocalDate.of(2025, 11, 16);
        LocalDate previousWeekStart = LocalDate.of(2025, 11, 3);
        LocalDate previousWeekEnd = LocalDate.of(2025, 11, 9);

        when(newsArticleRepository.getAverageSentiment(any(), any())).thenReturn(0.4);
        when(newsArticleRepository.countPositiveArticles(any(), any())).thenReturn(8L);
        when(newsArticleRepository.countNegativeArticles(any(), any())).thenReturn(2L);
        when(newsArticleRepository.countNeutralArticles(any(), any())).thenReturn(5L);
        when(sentimentAnalysisRepository.findByWeekStartDateAndWeekEndDate(weekStart, weekEnd))
            .thenReturn(Optional.empty());
        when(sentimentAnalysisRepository.findByWeekStartDateAndWeekEndDate(previousWeekStart, previousWeekEnd))
            .thenReturn(Optional.empty());

        SentimentAnalysis mockAnalysis = new SentimentAnalysis();
        mockAnalysis.setTrend("stable");
        when(sentimentAnalysisRepository.save(any(SentimentAnalysis.class))).thenReturn(mockAnalysis);

        SentimentAnalysis result = sentimentAnalysisService.calculateWeeklySentiment(weekStart, weekEnd);

        assertNotNull(result);
        assertEquals("stable", result.getTrend());
    }
}
