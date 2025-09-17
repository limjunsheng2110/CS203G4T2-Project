package com.cs205.tariffg4t2.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
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
    private Long id;
    private String name; // "USMCA", "CPTPP", etc.
    private String type;

    @ManyToMany
    private Set<Country> memberCountries;

    private LocalDate effectiveDate;
    private LocalDate expiryDate;
}