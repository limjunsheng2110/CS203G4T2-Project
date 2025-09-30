package com.cs205.tariffg4t2.model.basic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "preferential_rate")
public class PreferentialRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_agreement_id", nullable = false)
    private TradeAgreement tradeAgreement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_country_id", nullable = false)
    private Country originCountry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_country_id", nullable = false)
    private Country destinationCountry;

    @Column(name = "preferential_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal preferentialRate; // e.g., 0.05 for 5%

}
