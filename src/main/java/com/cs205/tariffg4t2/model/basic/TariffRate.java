
package com.cs205.tariffg4t2.model.basic;

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

    public enum TariffType { AD_VALOREM, SPECIFIC }

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
    private String unit; //kg, liters, units, etc.

    @Enumerated(EnumType.STRING)
    @Column(name = "tariff_type", nullable = false)
    private TariffType tariffType = TariffType.AD_VALOREM;

    @Column(name = "ad_valorem_rate", precision = 12, scale = 6, columnDefinition = "DECIMAL(12,6)", nullable = true)
    private BigDecimal adValoremRate;

    @Column(name = "specific_rate_amount", precision = 18, scale = 6)
    private BigDecimal specificRateAmount;
}
