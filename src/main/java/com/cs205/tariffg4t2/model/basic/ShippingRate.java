package com.cs205.tariffg4t2.model.basic;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "shipping_rates")
public class ShippingRate {

    // public enum ShippingMode { SEA, AIR, LAND }
    public enum ShippingRateType { FLAT, PER_WEIGHT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // country codes (match your 'country.code' values)
    @Column(name = "importing_country_code", nullable = false)
    private String importingCountryCode;

    @Column(name = "exporting_country_code", nullable = false)
    private String exportingCountryCode;

    // @Enumerated(EnumType.STRING)
    // @Column(name = "mode", nullable = false)
    // private ShippingMode mode = ShippingMode.SEA;
    @Column(name = "shipping_mode")
    private String shippingMode; 

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_type", nullable = false)
    private ShippingRateType rateType = ShippingRateType.FLAT;

    @Column(name = "amount", nullable = false, precision = 18, scale = 6)
    private BigDecimal amount; // flat amount or per-kg rate depending on rateType

    // @Column(name = "currency")
    // private String currency; // optional

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;
}

