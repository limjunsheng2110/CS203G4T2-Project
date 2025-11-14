package com.cs203.tariffg4t2.service.data;

import com.cs203.tariffg4t2.model.basic.NewsArticle;
import com.cs203.tariffg4t2.repository.basic.NewsArticleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsAPIServiceTest {

    @Mock
    private NewsArticleRepository newsArticleRepository;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private NewsAPIService newsAPIService;

    private String mockApiKey = "test-api-key-12345";
    private String mockApiUrl = "https://newsapi.org/v2";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(newsAPIService, "apiKey", mockApiKey);
        ReflectionTestUtils.setField(newsAPIService, "apiUrl", mockApiUrl);
        ReflectionTestUtils.setField(newsAPIService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(newsAPIService, "objectMapper", new ObjectMapper());
        
        // Mock embedding service to be unconfigured by default (prevents embedding generation in tests)
        // Use lenient() to avoid UnnecessaryStubbingException in tests that don't call this
        lenient().when(embeddingService.isConfigured()).thenReturn(false);
    }

    @Test
    void testIsConfigured_WithValidApiKey() {
        assertTrue(newsAPIService.isConfigured());
    }

    @Test
    void testIsConfigured_WithEmptyApiKey() {
        ReflectionTestUtils.setField(newsAPIService, "apiKey", "");
        assertFalse(newsAPIService.isConfigured());
    }

    @Test
    void testIsConfigured_WithNullApiKey() {
        ReflectionTestUtils.setField(newsAPIService, "apiKey", null);
        assertFalse(newsAPIService.isConfigured());
    }

    @Test
    void testFetchTradeNews_WithoutApiKey_ThrowsException() {
        ReflectionTestUtils.setField(newsAPIService, "apiKey", "");

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            newsAPIService.fetchTradeNews(7);
        });

        assertEquals("News API key not configured", exception.getMessage());
    }

    @Test
    void testFetchTradeNews_SuccessfulResponse() throws Exception {
        String mockResponse = """
            {
                "status": "ok",
                "totalResults": 2,
                "articles": [
                    {
                        "title": "Trade tariff news",
                        "description": "Important trade news about tariffs",
                        "url": "https://example.com/article1",
                        "source": {"name": "Test Source"},
                        "publishedAt": "2025-11-13T10:00:00Z"
                    },
                    {
                        "title": "Import export update",
                        "description": "News about imports and exports",
                        "url": "https://example.com/article2",
                        "source": {"name": "News Source"},
                        "publishedAt": "2025-11-12T15:00:00Z"
                    }
                ]
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);
        when(newsArticleRepository.existsByUrl(anyString())).thenReturn(false);

        List<NewsArticle> articles = newsAPIService.fetchTradeNews(7);

        assertNotNull(articles);
        assertEquals(2, articles.size());
        assertEquals("Trade tariff news", articles.get(0).getTitle());
        assertEquals("Import export update", articles.get(1).getTitle());
        assertTrue(articles.get(0).getKeywords().contains("tariff"));
    }

    @Test
    void testFetchTradeNews_WithCountries() throws Exception {
        String mockResponse = """
            {
                "status": "ok",
                "totalResults": 1,
                "articles": [
                    {
                        "title": "US China trade war",
                        "description": "Trade tensions between USA and China",
                        "url": "https://example.com/trade",
                        "source": {"name": "Global News"},
                        "publishedAt": "2025-11-13T12:00:00Z"
                    }
                ]
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);
        when(newsArticleRepository.existsByUrl(anyString())).thenReturn(false);

        List<NewsArticle> articles = newsAPIService.fetchTradeNews(7, "USA", "China");

        assertNotNull(articles);
        assertEquals(1, articles.size());
        assertEquals("US China trade war", articles.get(0).getTitle());
        assertTrue(articles.get(0).getKeywords().contains("trade war"));
    }

    @Test
    void testFetchTradeNews_ApiErrorResponse() {
        String errorResponse = """
            {
                "status": "error",
                "code": "apiKeyInvalid",
                "message": "Your API key is invalid"
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(errorResponse);

        Exception exception = assertThrows(Exception.class, () -> {
            newsAPIService.fetchTradeNews(7);
        });

        assertTrue(exception.getMessage().contains("News API error"));
        assertTrue(exception.getMessage().contains("apiKeyInvalid"));
    }

    @Test
    void testFetchTradeNews_RestClientException() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenThrow(new RestClientException("Connection timeout"));

        Exception exception = assertThrows(Exception.class, () -> {
            newsAPIService.fetchTradeNews(7);
        });

        assertTrue(exception.getMessage().contains("News API request failed"));
    }

    @Test
    void testFetchTradeNews_EmptyArticlesArray() throws Exception {
        String mockResponse = """
            {
                "status": "ok",
                "totalResults": 0,
                "articles": []
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);

        List<NewsArticle> articles = newsAPIService.fetchTradeNews(7);

        assertNotNull(articles);
        assertTrue(articles.isEmpty());
    }

    @Test
    void testFetchTradeNews_DuplicateArticles() throws Exception {
        String mockResponse = """
            {
                "status": "ok",
                "totalResults": 2,
                "articles": [
                    {
                        "title": "Trade news",
                        "description": "Trade description",
                        "url": "https://example.com/same-url",
                        "source": {"name": "Source"},
                        "publishedAt": "2025-11-13T10:00:00Z"
                    },
                    {
                        "title": "Another trade news",
                        "description": "Different content",
                        "url": "https://example.com/same-url",
                        "source": {"name": "Source"},
                        "publishedAt": "2025-11-13T11:00:00Z"
                    }
                ]
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);
        when(newsArticleRepository.existsByUrl("https://example.com/same-url"))
            .thenReturn(false)
            .thenReturn(true);

        List<NewsArticle> articles = newsAPIService.fetchTradeNews(7);

        assertNotNull(articles);
        assertEquals(1, articles.size());
    }

    @Test
    void testFetchTradeNews_NullFields() throws Exception {
        String mockResponse = """
            {
                "status": "ok",
                "totalResults": 1,
                "articles": [
                    {
                        "title": null,
                        "description": null,
                        "url": "https://example.com/article",
                        "source": {"name": "Source"},
                        "publishedAt": null
                    }
                ]
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);
        when(newsArticleRepository.existsByUrl(anyString())).thenReturn(false);

        List<NewsArticle> articles = newsAPIService.fetchTradeNews(7);

        assertNotNull(articles);
        assertEquals(1, articles.size());
        assertNull(articles.get(0).getTitle());
    }

    @Test
    void testFetchTradeNews_InvalidDateFormat() throws Exception {
        String mockResponse = """
            {
                "status": "ok",
                "totalResults": 1,
                "articles": [
                    {
                        "title": "News",
                        "description": "Description",
                        "url": "https://example.com/article",
                        "source": {"name": "Source"},
                        "publishedAt": "invalid-date-format"
                    }
                ]
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);
        when(newsArticleRepository.existsByUrl(anyString())).thenReturn(false);

        List<NewsArticle> articles = newsAPIService.fetchTradeNews(7);

        assertNotNull(articles);
        assertEquals(1, articles.size());
        assertNotNull(articles.get(0).getPublishedAt());
    }

    @Test
    void testTestAPIConnection_Success() throws Exception {
        String mockResponse = """
            {
                "status": "ok",
                "totalResults": 5,
                "articles": []
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);

        boolean result = newsAPIService.testAPIConnection();

        assertTrue(result);
    }

    @Test
    void testTestAPIConnection_NotConfigured() {
        ReflectionTestUtils.setField(newsAPIService, "apiKey", "");

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            newsAPIService.testAPIConnection();
        });

        assertEquals("News API key not configured", exception.getMessage());
    }

    @Test
    void testTestAPIConnection_Failure() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenThrow(new RestClientException("Connection failed"));

        assertThrows(Exception.class, () -> {
            newsAPIService.testAPIConnection();
        });
    }

    @Test
    void testExpandCountryName_USA() throws Exception {
        List<NewsArticle> articles = testCountryExpansion("USA");
        assertNotNull(articles);
    }

    @Test
    void testExpandCountryName_China() throws Exception {
        List<NewsArticle> articles = testCountryExpansion("China");
        assertNotNull(articles);
    }

    @Test
    void testExpandCountryName_UK() throws Exception {
        List<NewsArticle> articles = testCountryExpansion("UK");
        assertNotNull(articles);
    }

    @Test
    void testExpandCountryName_EU() throws Exception {
        List<NewsArticle> articles = testCountryExpansion("EU");
        assertNotNull(articles);
    }

    @Test
    void testExpandCountryName_Japan() throws Exception {
        List<NewsArticle> articles = testCountryExpansion("Japan");
        assertNotNull(articles);
    }

    @Test
    void testExpandCountryName_UnknownCountry() throws Exception {
        List<NewsArticle> articles = testCountryExpansion("Unknown");
        assertNotNull(articles);
    }

    private List<NewsArticle> testCountryExpansion(String country) throws Exception {
        String mockResponse = """
            {
                "status": "ok",
                "totalResults": 0,
                "articles": []
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);
        return newsAPIService.fetchTradeNews(7, country, "TestCountry");
    }

    @Test
    void testFetchTradeNews_AllKeywords() throws Exception {
        String mockResponse = """
            {
                "status": "ok",
                "totalResults": 1,
                "articles": [
                    {
                        "title": "Tariff and imports news",
                        "description": "Article about exports, customs, international trade, trade policy, and trade war",
                        "url": "https://example.com/keywords",
                        "source": {"name": "Source"},
                        "publishedAt": "2025-11-13T10:00:00Z"
                    }
                ]
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);
        when(newsArticleRepository.existsByUrl(anyString())).thenReturn(false);

        List<NewsArticle> articles = newsAPIService.fetchTradeNews(7);

        assertNotNull(articles);
        assertEquals(1, articles.size());
        String keywords = articles.get(0).getKeywords();
        assertTrue(keywords.contains("tariff"));
        assertTrue(keywords.contains("imports"));
        assertTrue(keywords.contains("exports"));
    }
}
