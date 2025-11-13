package com.cs203.tariffg4t2.repository.basic;

import com.cs203.tariffg4t2.model.basic.NewsArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {
    
    /**
     * Find articles published within a date range
     */
    List<NewsArticle> findByPublishedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find articles by country code
     */
    List<NewsArticle> findByCountryCodeOrderByPublishedAtDesc(String countryCode);
    
    /**
     * Find recent articles (last N days)
     */
    @Query("SELECT n FROM NewsArticle n WHERE n.publishedAt >= :since ORDER BY n.publishedAt DESC")
    List<NewsArticle> findRecentArticles(@Param("since") LocalDateTime since);
    
    /**
     * Find articles by URL to avoid duplicates
     */
    boolean existsByUrl(String url);
    
    /**
     * Get average sentiment for a date range
     */
    @Query("SELECT AVG(n.sentimentScore) FROM NewsArticle n " +
           "WHERE n.publishedAt BETWEEN :startDate AND :endDate")
    Double getAverageSentiment(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count articles by sentiment polarity
     */
    @Query("SELECT COUNT(n) FROM NewsArticle n " +
           "WHERE n.publishedAt BETWEEN :startDate AND :endDate " +
           "AND n.sentimentScore > 0.2")
    Long countPositiveArticles(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(n) FROM NewsArticle n " +
           "WHERE n.publishedAt BETWEEN :startDate AND :endDate " +
           "AND n.sentimentScore < -0.2")
    Long countNegativeArticles(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(n) FROM NewsArticle n " +
           "WHERE n.publishedAt BETWEEN :startDate AND :endDate " +
           "AND n.sentimentScore BETWEEN -0.2 AND 0.2")
    Long countNeutralArticles(@Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate);
    
    /**
     * Delete old articles (cleanup)
     */
    void deleteByPublishedAtBefore(LocalDateTime date);
}

