package com.cs203.tariffg4t2.model.basic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "exchange_rate", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"from_currency", "to_currency", "rate_date"}))
public class ExchangeRate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;  // ISO 4217 currency code (e.g., USD)
    
    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;    // ISO 4217 currency code (e.g., EUR)
    
    @Column(name = "rate", nullable = false, precision = 20, scale = 10)
    private BigDecimal rate;      // Exchange rate value
    
    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;   // Date of the exchange rate
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
    
    public ExchangeRate(String fromCurrency, String toCurrency, BigDecimal rate, LocalDate rateDate) {
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rate = rate;
        this.rateDate = rateDate;
    }
}

