package com.cs205.tariffg4t2.repository;

import com.cs205.tariffg4t2.model.api.TargetUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TargetUrlRepository extends JpaRepository<TargetUrl, Long> {
    
    // Find active URLs
    List<TargetUrl> findByIsActiveTrue();
    
    // Find URLs by site identifier
    Optional<TargetUrl> findBySiteIdentifier(String siteIdentifier);
    
    // Find URLs that need scraping (based on frequency and last scraped time)
    @Query("SELECT t FROM TargetUrl t WHERE t.isActive = true AND " +
           "(t.lastScraped IS NULL OR " +
           "(t.scrapeFrequency = 'DAILY' AND t.lastScraped < :dayAgo) OR " +
           "(t.scrapeFrequency = 'WEEKLY' AND t.lastScraped < :weekAgo) OR " +
           "(t.scrapeFrequency = 'MONTHLY' AND t.lastScraped < :monthAgo))")
    List<TargetUrl> findUrlsDueForScraping(LocalDateTime dayAgo, LocalDateTime weekAgo, LocalDateTime monthAgo);
    
    // Find URLs by frequency
    List<TargetUrl> findByScrapeFrequencyAndIsActiveTrue(String scrapeFrequency);
    
    // Count active URLs
    long countByIsActiveTrue();
}