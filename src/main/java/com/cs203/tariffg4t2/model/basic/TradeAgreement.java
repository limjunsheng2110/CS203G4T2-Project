package com.cs203.tariffg4t2.model.basic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class TradeAgreement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // "USMCA", "CPTPP", etc.

    @Column(nullable = false)
    private LocalDate effectiveDate;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @OneToMany(mappedBy = "tradeAgreement", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PreferentialRate> preferentialRates = new HashSet<>();

}