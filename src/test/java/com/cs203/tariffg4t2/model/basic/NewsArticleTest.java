package com.cs203.tariffg4t2.model.basic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class NewsArticleTest {

    private NewsArticle newsArticle;

    @BeforeEach
    void setUp() {
        newsArticle = new NewsArticle();
    }

    @Test
    void testDefaultConstructor() {
        assertNotNull(newsArticle);
        assertNull(newsArticle.getId());
        assertNull(newsArticle.getTitle());
    }

    @Test
    void testParameterizedConstructor() {
        LocalDateTime publishedAt = LocalDateTime.now();
        NewsArticle article = new NewsArticle(
            "Test Title",
            "Test Description",
            "https://test.com",
            "Test Source",
            publishedAt,
            0.5
        );

        assertEquals("Test Title", article.getTitle());
        assertEquals("Test Description", article.getDescription());
        assertEquals("https://test.com", article.getUrl());
        assertEquals("Test Source", article.getSource());
        assertEquals(publishedAt, article.getPublishedAt());
        assertEquals(0.5, article.getSentimentScore());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        NewsArticle article = new NewsArticle(
            1L,
            "Title",
            "Description",
            "https://url.com",
            "Source",
            now,
            0.8,
            "keywords",
            "US",
            now
        );

        assertEquals(1L, article.getId());
        assertEquals("Title", article.getTitle());
        assertEquals("Description", article.getDescription());
        assertEquals("https://url.com", article.getUrl());
        assertEquals("Source", article.getSource());
        assertEquals(now, article.getPublishedAt());
        assertEquals(0.8, article.getSentimentScore());
        assertEquals("keywords", article.getKeywords());
        assertEquals("US", article.getCountryCode());
        assertEquals(now, article.getCreatedAt());
    }

    @Test
    void testSettersAndGetters() {
        LocalDateTime publishedAt = LocalDateTime.now();

        newsArticle.setId(1L);
        newsArticle.setTitle("Trade News");
        newsArticle.setDescription("Important trade article");
        newsArticle.setUrl("https://example.com/article");
        newsArticle.setSource("Reuters");
        newsArticle.setPublishedAt(publishedAt);
        newsArticle.setSentimentScore(0.7);
        newsArticle.setKeywords("tariff, trade");
        newsArticle.setCountryCode("US");

        assertEquals(1L, newsArticle.getId());
        assertEquals("Trade News", newsArticle.getTitle());
        assertEquals("Important trade article", newsArticle.getDescription());
        assertEquals("https://example.com/article", newsArticle.getUrl());
        assertEquals("Reuters", newsArticle.getSource());
        assertEquals(publishedAt, newsArticle.getPublishedAt());
        assertEquals(0.7, newsArticle.getSentimentScore());
        assertEquals("tariff, trade", newsArticle.getKeywords());
        assertEquals("US", newsArticle.getCountryCode());
    }

    @Test
    void testSentimentScoreBoundaries() {
        newsArticle.setSentimentScore(-1.0);
        assertEquals(-1.0, newsArticle.getSentimentScore());

        newsArticle.setSentimentScore(1.0);
        assertEquals(1.0, newsArticle.getSentimentScore());

        newsArticle.setSentimentScore(0.0);
        assertEquals(0.0, newsArticle.getSentimentScore());
    }

    @Test
    void testNullValues() {
        newsArticle.setTitle(null);
        newsArticle.setDescription(null);
        newsArticle.setUrl(null);
        newsArticle.setSource(null);
        newsArticle.setPublishedAt(null);
        newsArticle.setSentimentScore(null);
        newsArticle.setKeywords(null);
        newsArticle.setCountryCode(null);

        assertNull(newsArticle.getTitle());
        assertNull(newsArticle.getDescription());
        assertNull(newsArticle.getUrl());
        assertNull(newsArticle.getSource());
        assertNull(newsArticle.getPublishedAt());
        assertNull(newsArticle.getSentimentScore());
        assertNull(newsArticle.getKeywords());
        assertNull(newsArticle.getCountryCode());
    }

    @Test
    void testEqualsAndHashCode() {
        NewsArticle article1 = new NewsArticle();
        article1.setId(1L);
        article1.setTitle("Test");
        article1.setUrl("https://test.com");

        NewsArticle article2 = new NewsArticle();
        article2.setId(1L);
        article2.setTitle("Test");
        article2.setUrl("https://test.com");

        assertEquals(article1, article2);
        assertEquals(article1.hashCode(), article2.hashCode());
    }

    @Test
    void testToString() {
        newsArticle.setTitle("Test Article");
        newsArticle.setUrl("https://test.com");

        String toString = newsArticle.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("Test Article") || toString.contains("NewsArticle"));
    }

    @Test
    void testLongTitle() {
        String longTitle = "A".repeat(500);
        newsArticle.setTitle(longTitle);
        assertEquals(longTitle, newsArticle.getTitle());
    }

    @Test
    void testLongDescription() {
        String longDescription = "B".repeat(2000);
        newsArticle.setDescription(longDescription);
        assertEquals(longDescription, newsArticle.getDescription());
    }

    @Test
    void testMultipleKeywords() {
        newsArticle.setKeywords("tariff, imports, exports, trade war, customs");
        assertEquals("tariff, imports, exports, trade war, customs", newsArticle.getKeywords());
    }

    @Test
    void testCountryCodeVariations() {
        newsArticle.setCountryCode("US");
        assertEquals("US", newsArticle.getCountryCode());

        newsArticle.setCountryCode("CN");
        assertEquals("CN", newsArticle.getCountryCode());

        newsArticle.setCountryCode("GB");
        assertEquals("GB", newsArticle.getCountryCode());
    }
}

