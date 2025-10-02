package com.CS203.tariffg4t2.model.basic;

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
    @JoinColumn(name = "importing_country", nullable = false)
    private Country importingCountry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exporting_country", nullable = false)
    private Country exportingCountry;

    @Column(name = "preferential_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal preferentialRate; // e.g., 0.05 for 5%

}
