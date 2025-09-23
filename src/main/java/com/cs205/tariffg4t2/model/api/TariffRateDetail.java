package com.cs205.tariffg4t2.model.api;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
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
    private LocalDateTime createdAt;
    
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
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public TariffRate getTariffRate() { return tariffRate; }
    public void setTariffRate(TariffRate tariffRate) { this.tariffRate = tariffRate; }
    
    public BigDecimal getFinalRate() { return finalRate; }
    public void setFinalRate(BigDecimal finalRate) { this.finalRate = finalRate; }
    
    public LocalDateTime getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDateTime effectiveDate) { this.effectiveDate = effectiveDate; }
    
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    
    public Long getTradeAgreementId() { return tradeAgreementId; }
    public void setTradeAgreementId(Long tradeAgreementId) { this.tradeAgreementId = tradeAgreementId; }
    
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public ScrapingJob getScraping() { return scraping; }
    public void setScraping(ScrapingJob scraping) { this.scraping = scraping; }
    
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}