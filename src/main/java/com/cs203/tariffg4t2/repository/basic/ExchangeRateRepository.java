package com.cs203.tariffg4t2.repository.basic;

import com.cs203.tariffg4t2.model.basic.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    
    /**
     * Find exchange rate for a specific currency pair and date
     */
    Optional<ExchangeRate> findByFromCurrencyAndToCurrencyAndRateDate(
        String fromCurrency, 
        String toCurrency, 
        LocalDate rateDate
    );
    
    /**
     * Find all exchange rates for a currency pair
     */
    List<ExchangeRate> findByFromCurrencyAndToCurrency(
        String fromCurrency,
        String toCurrency
    );

    /**
     * Find the most recent exchange rate for a currency pair
     */
    @Query("SELECT e FROM ExchangeRate e WHERE e.fromCurrency = :fromCurrency " +
           "AND e.toCurrency = :toCurrency ORDER BY e.rateDate DESC LIMIT 1")
    Optional<ExchangeRate> findLatestByFromCurrencyAndToCurrency(
        @Param("fromCurrency") String fromCurrency,
        @Param("toCurrency") String toCurrency
    );
    
    /**
     * Find all exchange rates for a currency pair within a date range
     */
    @Query("SELECT e FROM ExchangeRate e WHERE e.fromCurrency = :fromCurrency " +
           "AND e.toCurrency = :toCurrency AND e.rateDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.rateDate ASC")
    List<ExchangeRate> findByFromCurrencyAndToCurrencyAndRateDateBetween(
        @Param("fromCurrency") String fromCurrency,
        @Param("toCurrency") String toCurrency,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    /**
     * Delete old exchange rates (for cleanup)
     */
    void deleteByRateDateBefore(LocalDate date);
    
    /**
     * Count exchange rates for a currency pair in date range
     */
    @Query("SELECT COUNT(e) FROM ExchangeRate e WHERE e.fromCurrency = :fromCurrency " +
           "AND e.toCurrency = :toCurrency AND e.rateDate BETWEEN :startDate AND :endDate")
    long countByFromCurrencyAndToCurrencyAndRateDateBetween(
        @Param("fromCurrency") String fromCurrency,
        @Param("toCurrency") String toCurrency,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
