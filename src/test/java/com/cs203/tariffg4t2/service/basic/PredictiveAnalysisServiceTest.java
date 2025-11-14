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
        when(newsAPIService.fetchTradeNews(7)).thenReturn(newsArticles);
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
        verify(newsAPIService).fetchTradeNews(7);
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
        when(newsAPIService.fetchTradeNews(7)).thenThrow(new RuntimeException("API error"));
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
    private PredictiveAnalysisRequest request;
    private SentimentAnalysis mockSentiment;
    private ExchangeRate mockExchangeRate;

    @BeforeEach
    void setUp() {
        request = PredictiveAnalysisRequest.builder()
            .importingCountry("USA")
            .exportingCountry("China")
            .enableNewsAnalysis(false)
            .build();

        mockSentiment = new SentimentAnalysis();
        mockSentiment.setId(1L);
        mockSentiment.setAverageSentiment(0.5);
        mockSentiment.setTrend("improving");
        mockSentiment.setArticleCount(100);
        mockSentiment.setPositiveCount(60);
        mockSentiment.setNegativeCount(20);
        mockSentiment.setNeutralCount(20);
        mockSentiment.setWeekStartDate(LocalDate.now().minusDays(6));
        mockSentiment.setWeekEndDate(LocalDate.now());

        mockExchangeRate = new ExchangeRate();
        mockExchangeRate.setId(1L);
        mockExchangeRate.setFromCurrency("CNY");
        mockExchangeRate.setToCurrency("USD");
        mockExchangeRate.setRate(BigDecimal.valueOf(0.14));
        mockExchangeRate.setRateDate(LocalDate.now());
    }

    @Test
    void testAnalyzePrediction_WithoutNewsAnalysis_Success() {
        // Setup
        when(currencyCodeService.getCurrencyCode("USA")).thenReturn("USD");
        when(currencyCodeService.getCurrencyCode("CHINA")).thenReturn("CNY");
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(mockSentiment);
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("CNY", "USD"))
            .thenReturn(Optional.of(mockExchangeRate));
        when(sentimentAnalysisService.getSentimentHistory(4)).thenReturn(createMockHistory());
        when(newsArticleRepository.findRecentArticles(any())).thenReturn(new ArrayList<>());

        // Execute
        PredictiveAnalysisResponse response = predictiveAnalysisService.analyzePrediction(request);

        // Verify
        assertNotNull(response);
        assertEquals("USA", response.getImportingCountry());
        assertEquals("China", response.getExportingCountry());
        assertNotNull(response.getRecommendation());
        assertNotNull(response.getConfidenceScore());
        assertEquals(0.5, response.getCurrentSentiment());
        assertEquals("improving", response.getSentimentTrend());
        assertEquals(100, response.getArticlesAnalyzed());
        assertFalse(response.isLiveNewsAvailable());
    }

    @Test
    void testAnalyzePrediction_WithNewsAnalysis_Success() throws Exception {
        // Setup
        request.setEnableNewsAnalysis(true);
        List<NewsArticle> mockArticles = createMockArticles();

        when(newsAPIService.isConfigured()).thenReturn(true);
        when(newsAPIService.fetchTradeNews(anyInt(), anyString(), anyString())).thenReturn(mockArticles);
        when(currencyCodeService.getCurrencyCode("USA")).thenReturn("USD");
        when(currencyCodeService.getCurrencyCode("CHINA")).thenReturn("CNY");
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(mockSentiment);
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("CNY", "USD"))
            .thenReturn(Optional.of(mockExchangeRate));
        when(sentimentAnalysisService.getSentimentHistory(4)).thenReturn(createMockHistory());
        when(newsArticleRepository.findRecentArticles(any())).thenReturn(mockArticles);

        // Execute
        PredictiveAnalysisResponse response = predictiveAnalysisService.analyzePrediction(request);

        // Verify
        assertNotNull(response);
        assertTrue(response.isLiveNewsAvailable());
        assertEquals("live_api", response.getDataSource());
        verify(sentimentAnalysisService, times(1)).processArticleSentiments(mockArticles);
        verify(sentimentAnalysisService, times(1)).calculateWeeklySentiment(any(), any());
    }

    @Test
    void testAnalyzePrediction_NewsAPINotConfigured_FallsBack() {
        // Setup
        request.setEnableNewsAnalysis(true);

        when(newsAPIService.isConfigured()).thenReturn(false);
        when(currencyCodeService.getCurrencyCode("USA")).thenReturn("USD");
        when(currencyCodeService.getCurrencyCode("CHINA")).thenReturn("CNY");
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(mockSentiment);
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("CNY", "USD"))
            .thenReturn(Optional.of(mockExchangeRate));
        when(sentimentAnalysisService.getSentimentHistory(4)).thenReturn(createMockHistory());
        when(newsArticleRepository.findRecentArticles(any())).thenReturn(new ArrayList<>());

        // Execute
        PredictiveAnalysisResponse response = predictiveAnalysisService.analyzePrediction(request);

        // Verify
        assertNotNull(response);
        assertFalse(response.isLiveNewsAvailable());
        assertEquals("fallback_database", response.getDataSource());
        assertTrue(response.getMessage().contains("News API key is not configured"));
    }

    @Test
    void testAnalyzePrediction_CurrencyResolutionFails_ThrowsException() {
        // Setup
        when(currencyCodeService.getCurrencyCode("USA")).thenReturn(null);

        // Execute & Verify
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            predictiveAnalysisService.analyzePrediction(request);
        });

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("Could not resolve currencies"));
    }

    @Test
    void testAnalyzePrediction_NoSentimentData_ThrowsException() {
        // Setup
        when(currencyCodeService.getCurrencyCode("USA")).thenReturn("USD");
        when(currencyCodeService.getCurrencyCode("CHINA")).thenReturn("CNY");
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(null);

        // Execute & Verify
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            predictiveAnalysisService.analyzePrediction(request);
        });

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("No sentiment data available"));
    }

    @Test
    void testAnalyzePrediction_NoExchangeRateData_ThrowsException() {
        // Setup
        when(currencyCodeService.getCurrencyCode("USA")).thenReturn("USD");
        when(currencyCodeService.getCurrencyCode("CHINA")).thenReturn("CNY");
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(mockSentiment);
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("CNY", "USD"))
            .thenReturn(Optional.empty());

        // Execute & Verify
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            predictiveAnalysisService.analyzePrediction(request);
        });

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("No exchange rate data available"));
    }

    @Test
    void testAnalyzePrediction_BuyRecommendation_PositiveSentiment() {
        // Setup with very positive sentiment
        mockSentiment.setAverageSentiment(0.7);
        mockSentiment.setTrend("improving");

        when(currencyCodeService.getCurrencyCode("USA")).thenReturn("USD");
        when(currencyCodeService.getCurrencyCode("CHINA")).thenReturn("CNY");
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(mockSentiment);
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("CNY", "USD"))
            .thenReturn(Optional.of(mockExchangeRate));
        when(sentimentAnalysisService.getSentimentHistory(4)).thenReturn(createPositiveHistory());
        when(newsArticleRepository.findRecentArticles(any())).thenReturn(new ArrayList<>());

        // Execute
        PredictiveAnalysisResponse response = predictiveAnalysisService.analyzePrediction(request);

        // Verify
        assertNotNull(response);
        assertEquals("BUY", response.getRecommendation());
        assertTrue(response.getConfidenceScore() > 0.5);
    }

    @Test
    void testAnalyzePrediction_WaitRecommendation_NegativeSentiment() {
        // Setup with very negative sentiment
        mockSentiment.setAverageSentiment(-0.7);
        mockSentiment.setTrend("declining");

        when(currencyCodeService.getCurrencyCode("USA")).thenReturn("USD");
        when(currencyCodeService.getCurrencyCode("CHINA")).thenReturn("CNY");
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(mockSentiment);
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("CNY", "USD"))
            .thenReturn(Optional.of(mockExchangeRate));
        when(sentimentAnalysisService.getSentimentHistory(4)).thenReturn(createNegativeHistory());
        when(newsArticleRepository.findRecentArticles(any())).thenReturn(new ArrayList<>());

        // Execute
        PredictiveAnalysisResponse response = predictiveAnalysisService.analyzePrediction(request);

        // Verify
        assertNotNull(response);
        assertEquals("WAIT", response.getRecommendation());
    }

    @Test
    void testAnalyzePrediction_HoldRecommendation_NeutralSentiment() {
        // Setup with neutral sentiment
        mockSentiment.setAverageSentiment(0.0);
        mockSentiment.setTrend("stable");

        when(currencyCodeService.getCurrencyCode("USA")).thenReturn("USD");
        when(currencyCodeService.getCurrencyCode("CHINA")).thenReturn("CNY");
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(mockSentiment);
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("CNY", "USD"))
            .thenReturn(Optional.of(mockExchangeRate));
        when(sentimentAnalysisService.getSentimentHistory(4)).thenReturn(createNeutralHistory());
        when(newsArticleRepository.findRecentArticles(any())).thenReturn(new ArrayList<>());

        // Execute
        PredictiveAnalysisResponse response = predictiveAnalysisService.analyzePrediction(request);

        // Verify
        assertNotNull(response);
        assertEquals("HOLD", response.getRecommendation());
    }

    @Test
    void testGetDiagnostics_AllConfigured() {
        // Setup
        when(newsAPIService.isConfigured()).thenReturn(true);
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(mockSentiment);
        when(currencyCodeService.getCurrencyCode("USA")).thenReturn("USD");
        when(currencyCodeService.getCurrencyCode("CHINA")).thenReturn("CNY");
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("CNY", "USD"))
            .thenReturn(Optional.of(mockExchangeRate));

        // Execute
        PredictiveAnalysisDiagnosticsResponse diagnostics =
            predictiveAnalysisService.getDiagnostics("USA", "China", false);

        // Verify
        assertNotNull(diagnostics);
        assertTrue(diagnostics.isNewsApiKeyPresent());
        assertTrue(diagnostics.isSentimentDataAvailable());
        assertTrue(diagnostics.isExchangeRateAvailable());
        assertTrue(diagnostics.getWarnings().isEmpty());
    }

    @Test
    void testGetDiagnostics_NewsAPINotConfigured() {
        // Setup
        when(newsAPIService.isConfigured()).thenReturn(false);
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(mockSentiment);
        when(currencyCodeService.getCurrencyCode("USA")).thenReturn("USD");
        when(currencyCodeService.getCurrencyCode("CHINA")).thenReturn("CNY");
        when(exchangeRateRepository.findLatestByFromCurrencyAndToCurrency("CNY", "USD"))
            .thenReturn(Optional.of(mockExchangeRate));

        // Execute
        PredictiveAnalysisDiagnosticsResponse diagnostics =
            predictiveAnalysisService.getDiagnostics("USA", "China", false);

        // Verify
        assertNotNull(diagnostics);
        assertFalse(diagnostics.isNewsApiKeyPresent());
        assertFalse(diagnostics.getWarnings().isEmpty());
        assertTrue(diagnostics.getSuggestions().size() > 0);
    }

    @Test
    void testGetDiagnostics_NoSentimentData() {
        // Setup
        when(newsAPIService.isConfigured()).thenReturn(true);
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(null);
        when(currencyCodeService.getCurrencyCode("USA")).thenReturn("USD");
        when(currencyCodeService.getCurrencyCode("CHINA")).thenReturn("CNY");

        // Execute
        PredictiveAnalysisDiagnosticsResponse diagnostics =
            predictiveAnalysisService.getDiagnostics("USA", "China", false);

        // Verify
        assertNotNull(diagnostics);
        assertFalse(diagnostics.isSentimentDataAvailable());
        assertTrue(diagnostics.getWarnings().stream()
            .anyMatch(w -> w.contains("sentiment")));
    }

    @Test
    void testGetDiagnostics_WithNewsAPITest_Success() throws Exception {
        // Setup
        when(newsAPIService.isConfigured()).thenReturn(true);
        when(newsAPIService.testAPIConnection()).thenReturn(true);
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(mockSentiment);
        when(currencyCodeService.getCurrencyCode("USA")).thenReturn("USD");
        when(currencyCodeService.getCurrencyCode("CHINA")).thenReturn("CNY");

        // Execute
        PredictiveAnalysisDiagnosticsResponse diagnostics =
            predictiveAnalysisService.getDiagnostics("USA", "China", true);

        // Verify
        assertNotNull(diagnostics);
        assertTrue(diagnostics.getNewsApiReachable());
        verify(newsAPIService, times(1)).testAPIConnection();
    }

    @Test
    void testGetDiagnostics_WithNewsAPITest_Failure() throws Exception {
        // Setup
        when(newsAPIService.isConfigured()).thenReturn(true);
        when(newsAPIService.testAPIConnection()).thenThrow(new Exception("Connection failed"));
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(mockSentiment);
        when(currencyCodeService.getCurrencyCode("USA")).thenReturn("USD");
        when(currencyCodeService.getCurrencyCode("CHINA")).thenReturn("CNY");

        // Execute
        PredictiveAnalysisDiagnosticsResponse diagnostics =
            predictiveAnalysisService.getDiagnostics("USA", "China", true);

        // Verify
        assertNotNull(diagnostics);
        assertFalse(diagnostics.getNewsApiReachable());
        assertTrue(diagnostics.getWarnings().stream()
            .anyMatch(w -> w.contains("connection test failed")));
    }

    @Test
    void testGetDiagnostics_CurrencyResolutionFails() {
        // Setup
        when(newsAPIService.isConfigured()).thenReturn(true);
        when(sentimentAnalysisService.getLatestSentiment()).thenReturn(mockSentiment);
        when(currencyCodeService.getCurrencyCode("INVALID")).thenReturn(null);

        // Execute
        PredictiveAnalysisDiagnosticsResponse diagnostics =
            predictiveAnalysisService.getDiagnostics("INVALID", "China", false);

        // Verify
        assertNotNull(diagnostics);
        assertTrue(diagnostics.getWarnings().stream()
            .anyMatch(w -> w.contains("currency")));
    }

    @Test
    void testDebugFetchNews_Success() throws Exception {
        // Setup
        List<NewsArticle> mockArticles = createMockArticles();
        when(newsAPIService.fetchTradeNews(anyInt())).thenReturn(mockArticles);

        // Execute
        List<?> result = predictiveAnalysisService.debugFetchNews();

        // Verify
        assertNotNull(result);
        assertEquals(mockArticles.size(), result.size());
        verify(sentimentAnalysisService, times(1)).processArticleSentiments(any());
    }

    // Helper methods
    private List<NewsArticle> createMockArticles() {
        List<NewsArticle> articles = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            NewsArticle article = new NewsArticle();
            article.setId((long) i);
            article.setTitle("Trade news " + i);
            article.setDescription("Description " + i);
            article.setUrl("https://example.com/" + i);
            article.setSource("Source " + i);
            article.setPublishedAt(LocalDateTime.now().minusDays(i));
            article.setSentimentScore(0.5);
            articles.add(article);
        }
        return articles;
    }

    private List<SentimentAnalysis> createMockHistory() {
        List<SentimentAnalysis> history = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            SentimentAnalysis sa = new SentimentAnalysis();
            sa.setAverageSentiment(0.3 + (i * 0.05));
            sa.setTrend("improving");
            sa.setArticleCount(50);
            history.add(sa);
        }
        return history;
    }

    private List<SentimentAnalysis> createPositiveHistory() {
        List<SentimentAnalysis> history = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            SentimentAnalysis sa = new SentimentAnalysis();
            sa.setAverageSentiment(0.6 + (i * 0.05));
            sa.setTrend("improving");
            sa.setArticleCount(50);
            history.add(sa);
        }
        return history;
    }

    private List<SentimentAnalysis> createNegativeHistory() {
        List<SentimentAnalysis> history = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            SentimentAnalysis sa = new SentimentAnalysis();
            sa.setAverageSentiment(-0.6 - (i * 0.05));
            sa.setTrend("declining");
            sa.setArticleCount(50);
            history.add(sa);
        }
        return history;
    }

    private List<SentimentAnalysis> createNeutralHistory() {
        List<SentimentAnalysis> history = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            SentimentAnalysis sa = new SentimentAnalysis();
            sa.setAverageSentiment(0.0);
            sa.setTrend("stable");
            sa.setArticleCount(50);
            history.add(sa);
        }
        return history;
    }
}

