package com.cs205.tariffg4t2.model.api;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TargetUrl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String url;
    private String siteIdentifier;
    private String scrapeFrequency;
    private LocalDateTime lastScraped;
    private boolean isActive;
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "targetUrl")
    @JsonIgnore
    private List<ScrapingJob> scrapingJobs;

}

