package com.cs205.tariffg4t2.model.api;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@Table(name = "tariff_rate_details")
public class TariffRateDetail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Link to core tariff rate
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tariff_rate_id", nullable = false)
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
    private ScrapingJob scraping; // Link to the scraping job that found this rate
    
    // Additional metadata
    @Column(name = "confidence_score", precision = 3, scale = 2)
    private BigDecimal confidenceScore; // How confident we are in this data (0.00-1.00)
    
    @Column(name = "notes", length = 1000)
    private String notes;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // Constructors
    public TariffRateDetail() {}
    
    public TariffRateDetail(TariffRate tariffRate, BigDecimal finalRate, String dataSource) {
        this.tariffRate = tariffRate;
        this.finalRate = finalRate;
        this.dataSource = dataSource;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
    }
    
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
}