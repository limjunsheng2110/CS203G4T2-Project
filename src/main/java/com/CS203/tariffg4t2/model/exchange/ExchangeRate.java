package com.CS203.tariffg4t2.model.exchange;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "exchange_rates", 
       indexes = {
           @Index(name = "idx_currency_pair", columnList = "from_currency,to_currency"),
           @Index(name = "idx_timestamp", columnList = "timestamp")
       })
public class ExchangeRate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "from_currency", length = 3, nullable = false)
    private String fromCurrency; // e.g., "USD"
    
    @Column(name = "to_currency", length = 3, nullable = false)
    private String toCurrency; // e.g., "SGD"
    
    @Column(name = "rate", precision = 18, scale = 8, nullable = false)
    private BigDecimal rate; // Exchange rate value
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "source", length = 50)
    private String source; // API source name
    
    // Helper method to check if rate is stale (older than 24 hours)
    public boolean isStale() {
        return timestamp.isBefore(LocalDateTime.now().minusHours(24));
    }
}