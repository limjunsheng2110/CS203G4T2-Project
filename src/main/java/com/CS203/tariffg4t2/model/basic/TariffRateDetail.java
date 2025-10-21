package com.CS203.tariffg4t2.model.basic;

import com.CS203.tariffg4t2.model.web.ScrapingJob;
import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tariff_rate_details")
@ToString(onlyExplicitlyIncluded = true)  
public class TariffRateDetail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Link to core tariff rate
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tariff_rate_id", nullable = false)
    @JsonBackReference
    private TariffRate tariffRate;
    
    // Calculated/Final rate information
    @Column(name = "final_rate", precision = 10, scale = 4)
    private BigDecimal finalRate;
    
    @Column(name = "effective_date")
    private LocalDateTime effectiveDate;
    
    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;
    
    // Trade agreement information
    @Column(name = "trade_agreement_id")
    private Long tradeAgreementId;
    
    // Data source and audit fields
    @Column(name = "data_source")
    private String dataSource; // e.g., "singapore_customs", "manual_entry"
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Scraping metadata
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scraping_id")
    private ScrapingJob scraping; // Link to the scaping job that found this rate
    
    // Additional metadata
    @Column(name = "confidence_score", precision = 3, scale = 2)
    private BigDecimal confidenceScore; // How confident we are in this data (0.00-1.00)
    
    @Column(name = "notes", length = 1000)
    private String notes;
    
    @Column(name = "is_active")
    private Boolean isActive = true;

    
    // Lifecycle methods
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Column(name="trq_quota_balance")
    private BigDecimal trqQuotaBalance; // remaining in-quota amount (same unit as tariffRate.unitBasis)
    
    @Column(name="in_quota_rate_percent")
    private BigDecimal inQuotaRatePercent;
    
    @Column(name="in_quota_rate_specific")
    private BigDecimal inQuotaRateSpecific;
    
    @Column(name="out_quota_rate_percent")
    private BigDecimal outQuotaRatePercent;
    
    @Column(name="out_quota_rate_specific")
    private BigDecimal outQuotaRateSpecific;
    
    @Column(name="unit_basis")
    private String unitBasis; // "KG", "HEAD"
    
}