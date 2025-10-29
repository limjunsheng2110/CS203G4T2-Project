package com.cs203.tariffg4t2.model.basic;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "additional_duty_map")
public class AdditionalDutyMap {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length=10) private String importingCountry;
    @Column(length=10) private String exportingCountry;
    @Column(length=12) private String hsCode;

    @Column(precision=12, scale=6) private BigDecimal section301Rate;
    @Column(precision=12, scale=6) private BigDecimal antiDumpingRate;
    @Column(precision=12, scale=6) private BigDecimal countervailingRate;
    @Column(precision=12, scale=6) private BigDecimal safeguardRate;

    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}

