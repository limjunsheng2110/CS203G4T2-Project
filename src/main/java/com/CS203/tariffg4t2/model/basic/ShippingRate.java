package com.CS203.tariffg4t2.model.basic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shipping_rate")
public class ShippingRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // country codes (match your 'country.code' values)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "importing_country_code", nullable = false)
    private Country importingCountry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exporting_country_code", nullable = false)
    private Country exportingCountry;

    @Column(name = "air_rate", precision = 10, scale = 2)
    private BigDecimal airRate;

    @Column(name = "sea_rate", precision = 10, scale = 2)
    private BigDecimal seaRate;

    @Column(name = "land_rate", precision = 10, scale = 2, nullable = true)
    private BigDecimal landRate;

    @Column(name = "distance", precision = 10, scale = 2)
    private BigDecimal distance; // in kilometers
}

