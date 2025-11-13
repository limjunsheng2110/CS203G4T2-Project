package com.cs203.tariffg4t2.model.basic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sentiment_analysis", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"week_start_date", "week_end_date"}))
public class SentimentAnalysis {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;
    
    @Column(name = "week_end_date", nullable = false)
    private LocalDate weekEndDate;
    
    @Column(name = "average_sentiment", nullable = false)
    private Double averageSentiment;  // Weekly average: -1.0 to +1.0
    
    @Column(name = "article_count")
    private Integer articleCount;  // Number of articles analyzed
    
    @Column(name = "positive_count")
    private Integer positiveCount;  // Articles with positive sentiment
    
    @Column(name = "negative_count")
    private Integer negativeCount;  // Articles with negative sentiment
    
    @Column(name = "neutral_count")
    private Integer neutralCount;   // Articles with neutral sentiment
    
    @Column(name = "trend")
    private String trend;  // "improving", "declining", "stable"
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}

