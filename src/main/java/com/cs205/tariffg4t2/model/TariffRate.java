package com.cs205.tariffg4t2.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class TariffRate {
    @Id
    private Long id;

    @ManyToOne
    private Country importingCountry;

    @ManyToOne
    private Country exportingCountry;

    @ManyToOne
    private Product product;

    private BigDecimal baseRate;
    private BigDecimal finalRate; // after applying trade agreements

    private String rateType; // ad-valorem, specific etc.

    private LocalDate effectiveDate;
    private LocalDate expiryDate;

    @ManyToOne
    private TradeAgreement tradeAgreement; //null if there is no agreement, otherwise the name of the agreement.







}
