package com.cs203.tariffg4t2.repository.basic;

import com.cs203.tariffg4t2.model.basic.TariffRateDetail;
import com.cs203.tariffg4t2.model.basic.TariffRate;
import com.cs203.tariffg4t2.model.web.ScrapingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TariffRateDetailRepository extends JpaRepository<TariffRateDetail, Long> {
    
    // Find active details for a tariff rate
    List<TariffRateDetail> findByTariffRateAndIsActiveTrueOrderByCreatedAtDesc(TariffRate tariffRate);
    
    // Find details by scraping job
    List<TariffRateDetail> findByScraping(ScrapingJob scrapingJob);
    
    // Find latest detail for a tariff rate
    Optional<TariffRateDetail> findFirstByTariffRateAndIsActiveTrueOrderByCreatedAtDesc(TariffRate tariffRate);
    
    // Find details by data source
    List<TariffRateDetail> findByDataSourceAndIsActiveTrueOrderByCreatedAtDesc(String dataSource);
    
    // Find details created within date range
    List<TariffRateDetail> findByCreatedAtBetweenAndIsActiveTrue(LocalDateTime start, LocalDateTime end);
    
    // Get count of details by data source
    @Query("SELECT d.dataSource, COUNT(d) FROM TariffRateDetail d WHERE d.isActive = true GROUP BY d.dataSource")
    List<Object[]> countByDataSource();
    
    // Find details with low confidence scores
    List<TariffRateDetail> findByConfidenceScoreLessThanAndIsActiveTrueOrderByConfidenceScoreAsc(java.math.BigDecimal threshold);
    
    // Find details that are expiring soon
    @Query("SELECT d FROM TariffRateDetail d WHERE d.isActive = true AND d.expiryDate IS NOT NULL AND d.expiryDate BETWEEN :now AND :soon")
    List<TariffRateDetail> findExpiringDetails(@Param("now") LocalDateTime now, @Param("soon") LocalDateTime soon);
    
    // Find conflicting rates (same tariff rate, different final rates, overlapping validity)
    @Query("SELECT d1 FROM TariffRateDetail d1, TariffRateDetail d2 WHERE " +
           "d1.tariffRate = d2.tariffRate AND d1.id != d2.id AND " +
           "d1.isActive = true AND d2.isActive = true AND " +
           "d1.finalRate != d2.finalRate AND " +
           "((d1.effectiveDate <= d2.expiryDate OR d2.expiryDate IS NULL) AND " +
           "(d1.expiryDate >= d2.effectiveDate OR d1.expiryDate IS NULL))")
    List<TariffRateDetail> findConflictingRates();
}