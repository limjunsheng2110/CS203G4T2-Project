package com.cs205.tariffg4t2.model.api;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.List;

@Entity
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

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getSiteIdentifier() { return siteIdentifier; }
    public void setSiteIdentifier(String siteIdentifier) { this.siteIdentifier = siteIdentifier; }

    public LocalDateTime getLastScraped() { return lastScraped; }
    public void setLastScraped(LocalDateTime lastScraped) { this.lastScraped = lastScraped; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getScrapeFrequency() { return scrapeFrequency; }
    public void setScrapeFrequency(String scrapeFrequency) { this.scrapeFrequency = scrapeFrequency; }
    
    public List<ScrapingJob> getScrapingJobs() { return scrapingJobs; }
    public void setScrapingJobs(List<ScrapingJob> scrapingJobs) { this.scrapingJobs = scrapingJobs; }
}