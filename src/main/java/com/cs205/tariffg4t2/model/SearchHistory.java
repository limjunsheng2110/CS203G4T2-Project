package com.cs205.tariffg4t2.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "search_history")
public class SearchHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // many search histroies, one user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    // Many search histories, reference one country (importing)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "importing_country_code", nullable = false)
    private Country importingCountry;
    
    // many search histories can reference one country (exporting)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exporting_country_code", nullable = false)
    private Country exportingCountry;
    
    // store the tariff rate found at time of search
    @Column(name = "tariff_rate_found")
    private String tariffRateFound;
    
    // Search query details for debugging/analytics
    @Column(name = "search_query")
    private String searchQuery; // e.g., "USA -> China electronics"
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime searchedAt;
    
    // constructor for common searches
    public SearchHistory(User user, Country importingCountry, Country exportingCountry, String hsCode) {
        this.user = user;
        this.importingCountry = importingCountry;
        this.exportingCountry = exportingCountry;
        this.searchQuery = String.format("%s -> %s %s", 
            exportingCountry.getName(), 
            importingCountry.getName(), 
            hsCode != null ? hsCode : "general"
        );
    }
}