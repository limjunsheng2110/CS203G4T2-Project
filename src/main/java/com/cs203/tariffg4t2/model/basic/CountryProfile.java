package com.cs203.tariffg4t2.model.basic;

import com.cs203.tariffg4t2.model.enums.ValuationBasis;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "country_profile")
public class CountryProfile {
    @Id
    @Column(name = "country_code", length = 10)
    private String countryCode;

    @Enumerated(EnumType.STRING)
    private ValuationBasis valuationBasis;   // CIF or TRANSACTION

    @Column(precision=6, scale=4)
    private BigDecimal vatOrGstRate;         // e.g., 0.0900 for 9%

    @Column
    private Boolean vatIncludesDuties;       // true for EU/SG style VAT/GST base

    @Column
    private Boolean stackRemediesOnCV;       // whether extra duties (301/ADD/CVD) apply on customs value
}

