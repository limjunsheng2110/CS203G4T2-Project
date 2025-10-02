package com.CS203.tariffg4t2.repository.exchange;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.CS203.tariffg4t2.model.exchange.ExchangeRate;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    
    // Find the most recent exchange rate for a currency pair
    @Query("SELECT e FROM ExchangeRate e WHERE e.fromCurrency = :fromCurrency " +
           "AND e.toCurrency = :toCurrency ORDER BY e.timestamp DESC LIMIT 1")
    Optional<ExchangeRate> findLatestRate(
        @Param("fromCurrency") String fromCurrency,
        @Param("toCurrency") String toCurrency
    );
    
    // Find exchange rate within a time window (for cache validity check)
    @Query("SELECT e FROM ExchangeRate e WHERE e.fromCurrency = :fromCurrency " +
           "AND e.toCurrency = :toCurrency AND e.timestamp >= :after " +
           "ORDER BY e.timestamp DESC LIMIT 1")
    Optional<ExchangeRate> findRecentRate(
        @Param("fromCurrency") String fromCurrency,
        @Param("toCurrency") String toCurrency,
        @Param("after") LocalDateTime after
    );
    
    // Clean up old exchange rates (useful for maintenance)
    void deleteByTimestampBefore(LocalDateTime timestamp);
}