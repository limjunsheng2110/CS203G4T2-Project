package com.cs205.tariffg4t2.model.api;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "tariff_rates")
public class TariffRate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Core relationship fields
    @Column(name = "exporting_country_code", nullable = false)
    private String exportingCountryCode;
    
    @Column(name = "importing_country_code", nullable = false) 
    private String importingCountryCode;
    
    @Column(name = "hs_code", nullable = false)
    private String hsCode;
    
    // Core tariff data
    @Column(name = "base_rate", precision = 10, scale = 4)
    private BigDecimal baseRate;
    
    @Column(name = "unit")
    private String unit; // e.g., "per kg", "ad valorem", "per item"
    
    // One-to-many relationship with details
    @OneToMany(mappedBy = "tariffRate", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TariffRateDetail> details;
    
    // Constructors
    public TariffRate() {}
    
    public TariffRate(String exportingCountryCode, String importingCountryCode, 
                     String hsCode, BigDecimal baseRate) {
        this.exportingCountryCode = exportingCountryCode;
        this.importingCountryCode = importingCountryCode;
        this.hsCode = hsCode;
        this.baseRate = baseRate;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getExportingCountryCode() { return exportingCountryCode; }
    public void setExportingCountryCode(String exportingCountryCode) { this.exportingCountryCode = exportingCountryCode; }
    
    public String getImportingCountryCode() { return importingCountryCode; }
    public void setImportingCountryCode(String importingCountryCode) { this.importingCountryCode = importingCountryCode; }
    
    public String getHsCode() { return hsCode; }
    public void setHsCode(String hsCode) { this.hsCode = hsCode; }
    
    public BigDecimal getBaseRate() { return baseRate; }
    public void setBaseRate(BigDecimal baseRate) { this.baseRate = baseRate; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    public List<TariffRateDetail> getDetails() { return details; }
    public void setDetails(List<TariffRateDetail> details) { this.details = details; }
}