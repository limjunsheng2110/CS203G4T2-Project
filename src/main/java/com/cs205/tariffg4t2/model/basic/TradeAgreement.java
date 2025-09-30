package com.cs205.tariffg4t2.model.basic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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
    private String type;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "trade_agreement_countries",
        joinColumns = @JoinColumn(name = "trade_agreement_id"),
        inverseJoinColumns = @JoinColumn(name = "country_id")
    )
    private Set<Country> memberCountries;

    @Column(nullable = false)
    private LocalDate effectiveDate;

    private LocalDate expiryDate;
}