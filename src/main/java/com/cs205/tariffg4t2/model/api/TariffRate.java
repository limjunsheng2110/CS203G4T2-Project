package com.cs205.tariffg4t2.model.api;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tariff_rates")
public class TariffRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hs_code", nullable = false)
    private String hsCode;

    @Column(name = "importing_country_code")
    private String importingCountryCode;

    @Column(name = "exporting_country_code")
    private String exportingCountryCode;

    @Column(name = "base_rate", precision = 10, scale = 4)
    private BigDecimal baseRate;

    @Column(name = "unit")
    private String unit;

    @OneToMany(mappedBy = "tariffRate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TariffRateDetail> details;
}