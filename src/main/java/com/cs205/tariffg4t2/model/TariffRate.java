package com.cs205.tariffg4t2.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tariff_rates", 
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"importing_country_code", "exporting_country_code", "hs_code"}
       ))
public class TariffRate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Many tariff rates belong to one importing country
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "importing_country_code", nullable = false)
    @NotNull(message = "Importing country is required")
    private Country importingCountry;
    
    // Many tariff rates belong to one exporting country
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exporting_country_code", nullable = false)
    @NotNull(message = "Exporting country is required")
    private Country exportingCountry;
    
    // Many tariff rates can belong to one product
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hs_code", nullable = false)
    @NotNull(message = "Product is required")
    private Product product;
    
    @DecimalMin(value = "0.0", message = "Base rate must be non-negative")
    @Column(name = "base_rate", precision = 10, scale = 4, nullable = false)
    private BigDecimal baseRate; // Original tariff rate (e.g., 15.5%)
    
    @DecimalMin(value = "0.0", message = "Final rate must be non-negative")
    @Column(name = "final_rate", precision = 10, scale = 4)
    private BigDecimal finalRate; // After applying trade agreements
    
    @Enumerated(EnumType.STRING)
    @Column(name = "rate_type", nullable = false)
    private RateType rateType = RateType.AD_VALOREM;
    
    @Column(name = "effective_date")
    private LocalDate effectiveDate;
    
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    
    // Optional: Which trade agreement applies (if any)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_agreement_id")
    private TradeAgreement tradeAgreement;
    
    // Data source tracking
    @Column(name = "data_source")
    private String dataSource; // "WTO", "Manual", "API", etc.
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Audit field - who last updated this rate
    @Column(name = "updated_by")
    private String updatedBy; // Username or system identifier
    
    // Enum for tariff rate types
    public enum RateType {
        AD_VALOREM,    // Percentage of value (most common)
        SPECIFIC,      // Fixed amount per unit
        MIXED,         // Combination of both
        COMPOUND       // Alternative calculation method
    }
    
    // Helper method to calculate final rate
    public BigDecimal calculateFinalRate() {
        if (tradeAgreement != null && finalRate != null) {
            return finalRate;
        }
        return baseRate;
    }
    
    // Constructor for manual rate creation
    public TariffRate(Country importingCountry, Country exportingCountry, 
                     Product product, BigDecimal baseRate, RateType rateType) {
        this.importingCountry = importingCountry;
        this.exportingCountry = exportingCountry;
        this.product = product;
        this.baseRate = baseRate;
        this.finalRate = baseRate; // Initially same as base rate
        this.rateType = rateType;
        this.effectiveDate = LocalDate.now();
    }
}