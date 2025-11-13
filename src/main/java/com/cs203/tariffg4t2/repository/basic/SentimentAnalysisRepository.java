package com.cs203.tariffg4t2.repository.basic;

import com.cs203.tariffg4t2.model.basic.SentimentAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SentimentAnalysisRepository extends JpaRepository<SentimentAnalysis, Long> {
    
    /**
     * Find analysis for a specific week
     */
    Optional<SentimentAnalysis> findByWeekStartDateAndWeekEndDate(
        LocalDate weekStartDate, 
        LocalDate weekEndDate
    );
    
    /**
     * Get most recent sentiment analysis
     */
    @Query("SELECT s FROM SentimentAnalysis s ORDER BY s.weekEndDate DESC LIMIT 1")
    Optional<SentimentAnalysis> findLatest();
    
    /**
     * Get sentiment analyses within a date range
     */
    @Query("SELECT s FROM SentimentAnalysis s WHERE s.weekStartDate >= :startDate " +
           "AND s.weekEndDate <= :endDate ORDER BY s.weekStartDate ASC")
    List<SentimentAnalysis> findByDateRange(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);
    
    /**
     * Get last N weeks of sentiment data
     */
    @Query("SELECT s FROM SentimentAnalysis s ORDER BY s.weekEndDate DESC")
    List<SentimentAnalysis> findRecentWeeks();
    
    /**
     * Check if analysis exists for a week
     */
    boolean existsByWeekStartDateAndWeekEndDate(LocalDate weekStartDate, LocalDate weekEndDate);
}

