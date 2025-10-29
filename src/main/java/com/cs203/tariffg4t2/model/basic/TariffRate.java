
package com.cs203.tariffg4t2.model.basic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

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

    // @OneToMany(mappedBy = "tariffRate", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<TariffRateDetail> details;
        // DO NOT include in toString (would recurse)
    @OneToMany(mappedBy = "tariffRate", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonManagedReference              // helps Jackson avoid cycles on JSON
    @ToString.Exclude                  // 👈 prevent recursion
    @JsonIgnore                        // optional: completely hide from JSON if not needed
    private List<TariffRateDetail> details;

    @Column(name = "tariff_type", nullable = false)
    private String tariffType;

    @Column(name = "ad_valorem_rate", precision = 12, scale = 6, columnDefinition = "DECIMAL(12,6)")
    private BigDecimal adValoremRate;

    @Column(name = "specific_rate_amount", precision = 18, scale = 6)
    private BigDecimal specificRateAmount;

    @Column(name = "compound_percent", precision = 12, scale = 6)
    private BigDecimal compoundPercent;      // for COMPOUND: ad valorem leg

    @Column(name = "compound_specific", precision = 18, scale = 6)
    private BigDecimal compoundSpecific;     // for COMPOUND: specific leg (per unit)

    @Column(name = "mixed_percent", precision = 12, scale = 6)
    private BigDecimal mixedPercent;         // for MIXED: percent leg

    @Column(name = "mixed_specific", precision = 18, scale = 6)
    private BigDecimal mixedSpecific;        // for MIXED: specific leg (per unit)

    @Column(name = "unit_basis", length = 10)   // "HEAD" or "KG"
    private String unitBasis;

}
