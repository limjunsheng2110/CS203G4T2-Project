package com.cs203.tariffg4t2.model.basic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "country")
public class Country {
    @Id
    @Column(name = "country_code", length = 10)
    private String countryCode;
    
    @Column(name = "country_name", length = 100)  
    private String countryName;

    @Column(name = "iso3_code", length = 3)
    private String iso3Code;

    @Column(name = "vat_rate", precision = 6, scale = 4)
    private java.math.BigDecimal vatRate;

    // Constructor for backward compatibility
    public Country(String countryCode, String countryName) {
        this.countryCode = countryCode;
        this.countryName = countryName;
    }
}