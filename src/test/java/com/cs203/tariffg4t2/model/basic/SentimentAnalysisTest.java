package com.cs203.tariffg4t2.model.basic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SentimentAnalysisTest {

    private SentimentAnalysis sentimentAnalysis;

    @BeforeEach
    void setUp() {
        sentimentAnalysis = new SentimentAnalysis();
    }

    @Test
    void testDefaultConstructor() {
        assertNotNull(sentimentAnalysis);
        assertNull(sentimentAnalysis.getId());
        assertNull(sentimentAnalysis.getWeekStartDate());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDate startDate = LocalDate.of(2025, 11, 10);
        LocalDate endDate = LocalDate.of(2025, 11, 16);
        LocalDateTime now = LocalDateTime.now();

        SentimentAnalysis analysis = new SentimentAnalysis(
            1L,
            startDate,
            endDate,
            0.5,
            100,
            60,
            20,
            20,
            "improving",
            now,
            now
        );

        assertEquals(1L, analysis.getId());
        assertEquals(startDate, analysis.getWeekStartDate());
        assertEquals(endDate, analysis.getWeekEndDate());
        assertEquals(0.5, analysis.getAverageSentiment());
        assertEquals(100, analysis.getArticleCount());
        assertEquals(60, analysis.getPositiveCount());
        assertEquals(20, analysis.getNegativeCount());
        assertEquals(20, analysis.getNeutralCount());
        assertEquals("improving", analysis.getTrend());
    }

    @Test
    void testSettersAndGetters() {
        LocalDate startDate = LocalDate.of(2025, 11, 10);
        LocalDate endDate = LocalDate.of(2025, 11, 16);

        sentimentAnalysis.setId(1L);
        sentimentAnalysis.setWeekStartDate(startDate);
        sentimentAnalysis.setWeekEndDate(endDate);
        sentimentAnalysis.setAverageSentiment(0.6);
        sentimentAnalysis.setArticleCount(50);
        sentimentAnalysis.setPositiveCount(30);
        sentimentAnalysis.setNegativeCount(10);
        sentimentAnalysis.setNeutralCount(10);
        sentimentAnalysis.setTrend("stable");

        assertEquals(1L, sentimentAnalysis.getId());
        assertEquals(startDate, sentimentAnalysis.getWeekStartDate());
        assertEquals(endDate, sentimentAnalysis.getWeekEndDate());
        assertEquals(0.6, sentimentAnalysis.getAverageSentiment());
        assertEquals(50, sentimentAnalysis.getArticleCount());
        assertEquals(30, sentimentAnalysis.getPositiveCount());
        assertEquals(10, sentimentAnalysis.getNegativeCount());
        assertEquals(10, sentimentAnalysis.getNeutralCount());
        assertEquals("stable", sentimentAnalysis.getTrend());
    }

    @Test
    void testSentimentScoreBoundaries() {
        sentimentAnalysis.setAverageSentiment(-1.0);
        assertEquals(-1.0, sentimentAnalysis.getAverageSentiment());

        sentimentAnalysis.setAverageSentiment(1.0);
        assertEquals(1.0, sentimentAnalysis.getAverageSentiment());

        sentimentAnalysis.setAverageSentiment(0.0);
        assertEquals(0.0, sentimentAnalysis.getAverageSentiment());
    }

    @Test
    void testTrendValues() {
        sentimentAnalysis.setTrend("improving");
        assertEquals("improving", sentimentAnalysis.getTrend());

        sentimentAnalysis.setTrend("declining");
        assertEquals("declining", sentimentAnalysis.getTrend());

        sentimentAnalysis.setTrend("stable");
        assertEquals("stable", sentimentAnalysis.getTrend());
    }

    @Test
    void testArticleCounts() {
        sentimentAnalysis.setPositiveCount(50);
        sentimentAnalysis.setNegativeCount(30);
        sentimentAnalysis.setNeutralCount(20);
        sentimentAnalysis.setArticleCount(100);

        assertEquals(100, sentimentAnalysis.getArticleCount());
        assertEquals(50, sentimentAnalysis.getPositiveCount());
        assertEquals(30, sentimentAnalysis.getNegativeCount());
        assertEquals(20, sentimentAnalysis.getNeutralCount());
    }

    @Test
    void testZeroCounts() {
        sentimentAnalysis.setPositiveCount(0);
        sentimentAnalysis.setNegativeCount(0);
        sentimentAnalysis.setNeutralCount(0);
        sentimentAnalysis.setArticleCount(0);

        assertEquals(0, sentimentAnalysis.getArticleCount());
        assertEquals(0, sentimentAnalysis.getPositiveCount());
        assertEquals(0, sentimentAnalysis.getNegativeCount());
        assertEquals(0, sentimentAnalysis.getNeutralCount());
    }

    @Test
    void testDateRange() {
        LocalDate start = LocalDate.of(2025, 11, 1);
        LocalDate end = LocalDate.of(2025, 11, 7);

        sentimentAnalysis.setWeekStartDate(start);
        sentimentAnalysis.setWeekEndDate(end);

        assertEquals(start, sentimentAnalysis.getWeekStartDate());
        assertEquals(end, sentimentAnalysis.getWeekEndDate());
        assertEquals(7, java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1);
    }

    @Test
    void testTimestamps() {
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now().plusHours(1);

        sentimentAnalysis.setCreatedAt(createdAt);
        sentimentAnalysis.setUpdatedAt(updatedAt);

        assertEquals(createdAt, sentimentAnalysis.getCreatedAt());
        assertEquals(updatedAt, sentimentAnalysis.getUpdatedAt());
    }

    @Test
    void testNullValues() {
        sentimentAnalysis.setId(null);
        sentimentAnalysis.setWeekStartDate(null);
        sentimentAnalysis.setWeekEndDate(null);
        sentimentAnalysis.setAverageSentiment(null);
        sentimentAnalysis.setArticleCount(null);
        sentimentAnalysis.setPositiveCount(null);
        sentimentAnalysis.setNegativeCount(null);
        sentimentAnalysis.setNeutralCount(null);
        sentimentAnalysis.setTrend(null);

        assertNull(sentimentAnalysis.getId());
        assertNull(sentimentAnalysis.getWeekStartDate());
        assertNull(sentimentAnalysis.getWeekEndDate());
        assertNull(sentimentAnalysis.getAverageSentiment());
        assertNull(sentimentAnalysis.getArticleCount());
        assertNull(sentimentAnalysis.getPositiveCount());
        assertNull(sentimentAnalysis.getNegativeCount());
        assertNull(sentimentAnalysis.getNeutralCount());
        assertNull(sentimentAnalysis.getTrend());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDate start = LocalDate.of(2025, 11, 10);
        LocalDate end = LocalDate.of(2025, 11, 16);

        SentimentAnalysis analysis1 = new SentimentAnalysis();
        analysis1.setId(1L);
        analysis1.setWeekStartDate(start);
        analysis1.setWeekEndDate(end);
        analysis1.setAverageSentiment(0.5);

        SentimentAnalysis analysis2 = new SentimentAnalysis();
        analysis2.setId(1L);
        analysis2.setWeekStartDate(start);
        analysis2.setWeekEndDate(end);
        analysis2.setAverageSentiment(0.5);

        assertEquals(analysis1, analysis2);
        assertEquals(analysis1.hashCode(), analysis2.hashCode());
    }

    @Test
    void testToString() {
        sentimentAnalysis.setId(1L);
        sentimentAnalysis.setAverageSentiment(0.5);
        sentimentAnalysis.setTrend("improving");

        String toString = sentimentAnalysis.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("SentimentAnalysis") || toString.contains("1"));
    }

    @Test
    void testNegativeSentiment() {
        sentimentAnalysis.setAverageSentiment(-0.8);
        sentimentAnalysis.setNegativeCount(80);
        sentimentAnalysis.setPositiveCount(10);
        sentimentAnalysis.setNeutralCount(10);

        assertTrue(sentimentAnalysis.getAverageSentiment() < 0);
        assertTrue(sentimentAnalysis.getNegativeCount() > sentimentAnalysis.getPositiveCount());
    }

    @Test
    void testPositiveSentiment() {
        sentimentAnalysis.setAverageSentiment(0.7);
        sentimentAnalysis.setPositiveCount(70);
        sentimentAnalysis.setNegativeCount(20);
        sentimentAnalysis.setNeutralCount(10);

        assertTrue(sentimentAnalysis.getAverageSentiment() > 0);
        assertTrue(sentimentAnalysis.getPositiveCount() > sentimentAnalysis.getNegativeCount());
    }

    @Test
    void testNeutralSentiment() {
        sentimentAnalysis.setAverageSentiment(0.0);
        sentimentAnalysis.setPositiveCount(33);
        sentimentAnalysis.setNegativeCount(33);
        sentimentAnalysis.setNeutralCount(34);

        assertEquals(0.0, sentimentAnalysis.getAverageSentiment());
    }

    @Test
    void testLargeCounts() {
        sentimentAnalysis.setArticleCount(10000);
        sentimentAnalysis.setPositiveCount(6000);
        sentimentAnalysis.setNegativeCount(3000);
        sentimentAnalysis.setNeutralCount(1000);

        assertEquals(10000, sentimentAnalysis.getArticleCount());
        assertEquals(6000, sentimentAnalysis.getPositiveCount());
    }

    @Test
    void testWeekSpan() {
        LocalDate monday = LocalDate.of(2025, 11, 10);
        LocalDate sunday = LocalDate.of(2025, 11, 16);

        sentimentAnalysis.setWeekStartDate(monday);
        sentimentAnalysis.setWeekEndDate(sunday);

        long days = java.time.temporal.ChronoUnit.DAYS.between(
            sentimentAnalysis.getWeekStartDate(),
            sentimentAnalysis.getWeekEndDate()
        );

        assertEquals(6, days);
    }
}

