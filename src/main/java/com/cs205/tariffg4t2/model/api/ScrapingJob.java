package com.cs205.tariffg4t2.model.api;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ScrapingJob {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "target_url_id")
    private TargetUrl targetUrl;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // SUCCESS, FAILED, IN_PROGRESS
    private int recordsExtracted;
    private String errorMessage;
    
    // Constructors
    public ScrapingJob() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public TargetUrl getTargetUrl() { return targetUrl; }
    public void setTargetUrl(TargetUrl targetUrl) { this.targetUrl = targetUrl; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public int getRecordsExtracted() { return recordsExtracted; }
    public void setRecordsExtracted(int recordsExtracted) { this.recordsExtracted = recordsExtracted; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}