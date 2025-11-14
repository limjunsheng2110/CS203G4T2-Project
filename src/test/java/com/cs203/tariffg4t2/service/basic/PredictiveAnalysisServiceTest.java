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
        assertTrue(response.getMessage().contains("Historical data fetched from database"));
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
