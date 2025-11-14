package com.cs203.tariffg4t2.service.basic;

import com.cs203.tariffg4t2.dto.request.PredictiveAnalysisRequest;
import com.cs203.tariffg4t2.dto.response.PredictiveAnalysisDiagnosticsResponse;
import com.cs203.tariffg4t2.dto.response.PredictiveAnalysisResponse;
import com.cs203.tariffg4t2.model.basic.ExchangeRate;
import com.cs203.tariffg4t2.model.basic.NewsArticle;
import com.cs203.tariffg4t2.model.basic.SentimentAnalysis;
import com.cs203.tariffg4t2.repository.basic.ExchangeRateRepository;
import com.cs203.tariffg4t2.repository.basic.NewsArticleRepository;
import com.cs203.tariffg4t2.service.data.CurrencyCodeService;
import com.cs203.tariffg4t2.service.data.NewsAPIService;
import com.cs203.tariffg4t2.service.data.SentimentAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PredictiveAnalysisServiceTest {

    @Mock
    private NewsAPIService newsAPIService;

    @Mock
    private SentimentAnalysisService sentimentAnalysisService;

    @Mock
    private NewsArticleRepository newsArticleRepository;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private CurrencyCodeService currencyCodeService;

    @InjectMocks
    private PredictiveAnalysisService predictiveAnalysisService;

    private SentimentAnalysis currentSentiment;
    private ExchangeRate currentExchangeRate;
    private List<SentimentAnalysis> sentimentHistory;
    private List<NewsArticle> newsArticles;

    @BeforeEach
    void setUp() {
        // Set up current sentiment
        currentSentiment = new SentimentAnalysis();
        currentSentiment.setAverageSentiment(0.5);
        currentSentiment.setTrend("improving");
        currentSentiment.setArticleCount(10);
        currentSentiment.setPositiveCount(7);
        currentSentiment.setNegativeCount(2);
        currentSentiment.setNeutralCount(1);
        currentSentiment.setWeekStartDate(LocalDate.now().minusDays(6));
        currentSentiment.setWeekEndDate(LocalDate.now());

        // Set up exchange rate
        currentExchangeRate = new ExchangeRate();
        currentExchangeRate.setFromCurrency("USD");
        currentExchangeRate.setToCurrency("SGD");
        currentExchangeRate.setRate(new BigDecimal("1.35"));
        currentExchangeRate.setRateDate(LocalDate.now());

        // Set up sentiment history
        sentimentHistory = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            SentimentAnalysis historical = new SentimentAnalysis();
            historical.setAverageSentiment(0.3 + (i * 0.05));
            historical.setTrend("stable");
            historical.setArticleCount(10);
            historical.setWeekStartDate(LocalDate.now().minusWeeks(i + 1));
            historical.setWeekEndDate(LocalDate.now().minusWeeks(i));
            sentimentHistory.add(historical);
        }

        // Set up news articles
        newsArticles = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            NewsArticle article = new NewsArticle();
            article.setTitle("Trade news headline " + i);
            article.setDescription("Trade news description " + i);
            article.setSentimentScore(0.5);
            article.setPublishedAt(LocalDateTime.now().minusDays(i));
            newsArticles.add(article);
        }
    }

    @Test
    void analyzePrediction_Success_WithNewsAnalysis() throws Exception {
        // Given
        PredictiveAnalysisRequest request = new PredictiveAnalysisRequest();
        request.setImportingCountry("SG");
        request.setExportingCountry("US");
        request.setEnableNewsAnalysis(true);

        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(newsAPIService.isConfigured()).thenReturn(true);
        when(newsAPIService.fetchTradeNews(30, "SG", "US")).thenReturn(newsArticles);
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(currentSentiment);
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.of(currentExchangeRate));
        when(sentimentAnalysisService.getSentimentHistory(4)).thenReturn(sentimentHistory);
        when(newsArticleRepository.findRecentArticles(any(LocalDateTime.class)))
                .thenReturn(newsArticles);

        // When
        PredictiveAnalysisResponse response = predictiveAnalysisService.analyzePrediction(request);

        // Then
        assertNotNull(response);
        assertEquals("SG", response.getImportingCountry());
        assertEquals("US", response.getExportingCountry());
        assertNotNull(response.getRecommendation());
        assertNotNull(response.getConfidenceScore());
        assertNotNull(response.getRationale());
        assertEquals(0.5, response.getCurrentSentiment());
        assertEquals("improving", response.getSentimentTrend());
        assertTrue(response.isLiveNewsAvailable());
        verify(newsAPIService).fetchTradeNews(30, "SG", "US");
        verify(sentimentAnalysisService).processArticleSentiments(newsArticles);
    }

    @Test
    void analyzePrediction_Success_WithoutNewsAnalysis() throws Exception {
        // Given
        PredictiveAnalysisRequest request = new PredictiveAnalysisRequest();
        request.setImportingCountry("SG");
        request.setExportingCountry("US");
        request.setEnableNewsAnalysis(false);

        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(currentSentiment);
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.of(currentExchangeRate));
        when(sentimentAnalysisService.getSentimentHistory(4)).thenReturn(sentimentHistory);
        when(newsArticleRepository.findRecentArticles(any(LocalDateTime.class)))
                .thenReturn(newsArticles);

        // When
        PredictiveAnalysisResponse response = predictiveAnalysisService.analyzePrediction(request);

        // Then
        assertNotNull(response);
        assertFalse(response.isLiveNewsAvailable());
        verify(newsAPIService, never()).fetchTradeNews(anyInt());
    }

    @Test
    void analyzePrediction_InvalidCurrency() {
        // Given
        PredictiveAnalysisRequest request = new PredictiveAnalysisRequest();
        request.setImportingCountry("INVALID");
        request.setExportingCountry("US");

        when(currencyCodeService.getCurrencyCode("INVALID")).thenReturn(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                predictiveAnalysisService.analyzePrediction(request));
    }

    @Test
    void analyzePrediction_NoSentimentData() {
        // Given
        PredictiveAnalysisRequest request = new PredictiveAnalysisRequest();
        request.setImportingCountry("SG");
        request.setExportingCountry("US");

        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(null);

        // When & Then
        assertThrows(RuntimeException.class, () ->
                predictiveAnalysisService.analyzePrediction(request));
    }

    @Test
    void analyzePrediction_NoExchangeRateData() {
        // Given
        PredictiveAnalysisRequest request = new PredictiveAnalysisRequest();
        request.setImportingCountry("SG");
        request.setExportingCountry("US");

        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(currentSentiment);
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () ->
                predictiveAnalysisService.analyzePrediction(request));
    }

    @Test
    void analyzePrediction_BuyRecommendation() {
        // Given - Very positive sentiment
        currentSentiment.setAverageSentiment(0.7);
        currentSentiment.setTrend("improving");

        PredictiveAnalysisRequest request = new PredictiveAnalysisRequest();
        request.setImportingCountry("SG");
        request.setExportingCountry("US");

        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(currentSentiment);
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.of(currentExchangeRate));
        when(sentimentAnalysisService.getSentimentHistory(4)).thenReturn(sentimentHistory);
        when(newsArticleRepository.findRecentArticles(any(LocalDateTime.class)))
                .thenReturn(newsArticles);

        // When
        PredictiveAnalysisResponse response = predictiveAnalysisService.analyzePrediction(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getRecommendation());
    }

    @Test
    void analyzePrediction_WaitRecommendation() {
        // Given - Very negative sentiment
        currentSentiment.setAverageSentiment(-0.7);
        currentSentiment.setTrend("declining");

        PredictiveAnalysisRequest request = new PredictiveAnalysisRequest();
        request.setImportingCountry("SG");
        request.setExportingCountry("US");

        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(currentSentiment);
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.of(currentExchangeRate));
        when(sentimentAnalysisService.getSentimentHistory(4)).thenReturn(sentimentHistory);
        when(newsArticleRepository.findRecentArticles(any(LocalDateTime.class)))
                .thenReturn(newsArticles);

        // When
        PredictiveAnalysisResponse response = predictiveAnalysisService.analyzePrediction(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getRecommendation());
    }

    @Test
    void analyzePrediction_HoldRecommendation() {
        // Given - Neutral sentiment
        currentSentiment.setAverageSentiment(0.1);
        currentSentiment.setTrend("stable");

        PredictiveAnalysisRequest request = new PredictiveAnalysisRequest();
        request.setImportingCountry("SG");
        request.setExportingCountry("US");

        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(currentSentiment);
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.of(currentExchangeRate));
        when(sentimentAnalysisService.getSentimentHistory(4)).thenReturn(sentimentHistory);
        when(newsArticleRepository.findRecentArticles(any(LocalDateTime.class)))
                .thenReturn(newsArticles);

        // When
        PredictiveAnalysisResponse response = predictiveAnalysisService.analyzePrediction(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getRecommendation());
    }

    @Test
    void analyzePrediction_WithSentimentHistory() {
        // Given
        PredictiveAnalysisRequest request = new PredictiveAnalysisRequest();
        request.setImportingCountry("SG");
        request.setExportingCountry("US");

        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(currentSentiment);
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.of(currentExchangeRate));
        when(sentimentAnalysisService.getSentimentHistory(4)).thenReturn(sentimentHistory);
        when(newsArticleRepository.findRecentArticles(any(LocalDateTime.class)))
                .thenReturn(newsArticles);

        // When
        PredictiveAnalysisResponse response = predictiveAnalysisService.analyzePrediction(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getSentimentHistory());
        assertFalse(response.getSentimentHistory().isEmpty());
    }

    @Test
    void analyzePrediction_WithSupportingHeadlines() {
        // Given
        PredictiveAnalysisRequest request = new PredictiveAnalysisRequest();
        request.setImportingCountry("SG");
        request.setExportingCountry("US");

        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(currentSentiment);
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.of(currentExchangeRate));
        when(sentimentAnalysisService.getSentimentHistory(4)).thenReturn(sentimentHistory);
        when(newsArticleRepository.findRecentArticles(any(LocalDateTime.class)))
                .thenReturn(newsArticles);

        // When
        PredictiveAnalysisResponse response = predictiveAnalysisService.analyzePrediction(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getSupportingHeadlines());
    }

    @Test
    void getDiagnostics_AllDataAvailable() throws Exception {
        // Given
        when(newsAPIService.isConfigured()).thenReturn(true);
        when(newsAPIService.testAPIConnection()).thenReturn(true);
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(currentSentiment);
        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.of(currentExchangeRate));

        // When
        PredictiveAnalysisDiagnosticsResponse response =
                predictiveAnalysisService.getDiagnostics("SG", "US", true);

        // Then
        assertNotNull(response);
        assertTrue(response.isNewsApiKeyPresent());
        assertTrue(response.getNewsApiReachable());
        assertTrue(response.isSentimentDataAvailable());
        assertTrue(response.isExchangeRateAvailable());
        assertTrue(response.getWarnings().isEmpty());
    }

    @Test
    void getDiagnostics_NewsApiNotConfigured() {
        // Given
        when(newsAPIService.isConfigured()).thenReturn(false);
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(currentSentiment);
        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.of(currentExchangeRate));

        // When
        PredictiveAnalysisDiagnosticsResponse response =
                predictiveAnalysisService.getDiagnostics("SG", "US", false);

        // Then
        assertNotNull(response);
        assertFalse(response.isNewsApiKeyPresent());
        assertFalse(response.getWarnings().isEmpty());
        assertFalse(response.getSuggestions().isEmpty());
    }

    @Test
    void getDiagnostics_NoSentimentData() {
        // Given
        when(newsAPIService.isConfigured()).thenReturn(true);
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(null);
        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.of(currentExchangeRate));

        // When
        PredictiveAnalysisDiagnosticsResponse response =
                predictiveAnalysisService.getDiagnostics("SG", "US", false);

        // Then
        assertNotNull(response);
        assertFalse(response.isSentimentDataAvailable());
        assertFalse(response.getWarnings().isEmpty());
    }

    @Test
    void getDiagnostics_NoExchangeRateData() {
        // Given
        when(newsAPIService.isConfigured()).thenReturn(true);
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(currentSentiment);
        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.empty());

        // When
        PredictiveAnalysisDiagnosticsResponse response =
                predictiveAnalysisService.getDiagnostics("SG", "US", false);

        // Then
        assertNotNull(response);
        assertFalse(response.isExchangeRateAvailable());
        assertFalse(response.getWarnings().isEmpty());
    }

    @Test
    void getDiagnostics_InvalidCurrency() {
        // Given
        when(newsAPIService.isConfigured()).thenReturn(true);
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(currentSentiment);
        when(currencyCodeService.getCurrencyCode("INVALID")).thenReturn(null);

        // When
        PredictiveAnalysisDiagnosticsResponse response =
                predictiveAnalysisService.getDiagnostics("INVALID", "US", false);

        // Then
        assertNotNull(response);
        assertFalse(response.getWarnings().isEmpty());
        assertFalse(response.getSuggestions().isEmpty());
    }

    @Test
    void analyzePrediction_NewsApiFails() throws Exception {
        // Given
        PredictiveAnalysisRequest request = new PredictiveAnalysisRequest();
        request.setImportingCountry("SG");
        request.setExportingCountry("US");
        request.setEnableNewsAnalysis(true);

        when(currencyCodeService.getCurrencyCode("SG")).thenReturn("SGD");
        when(currencyCodeService.getCurrencyCode("US")).thenReturn("USD");
        when(newsAPIService.isConfigured()).thenReturn(true);
        when(newsAPIService.fetchTradeNews(30, "SG", "US")).thenThrow(new RuntimeException("API error"));
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(currentSentiment);
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("USD", "SGD"))
                .thenReturn(Optional.of(currentExchangeRate));
        when(sentimentAnalysisService.getSentimentHistory(4)).thenReturn(sentimentHistory);
        when(newsArticleRepository.findRecentArticles(any(LocalDateTime.class)))
                .thenReturn(newsArticles);


        // When
        PredictiveAnalysisResponse response = predictiveAnalysisService.analyzePrediction(request);

        // Then
        assertNotNull(response);
        assertFalse(response.isLiveNewsAvailable());
        assertNotNull(response.getMessage());
        assertTrue(response.getMessage().contains("unavailable"));
    }
}
