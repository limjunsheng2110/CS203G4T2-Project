package com.cs203.tariffg4t2.model.basic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "news_article")
public class NewsArticle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "title", length = 500)
    private String title;
    
    @Column(name = "description", length = 2000)
    private String description;
    
    @Column(name = "url", length = 500)
    private String url;
    
    @Column(name = "source")
    private String source;
    
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    
    @Column(name = "sentiment_score", precision = 5, scale = 4)
    private Double sentimentScore;  // Range: -1.0 (negative) to +1.0 (positive)
    
    @Column(name = "keywords")
    private String keywords;  // Matched keywords: tariff, imports, etc.
    
    @Column(name = "country_code", length = 10)
    private String countryCode;  // Related country if applicable
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public NewsArticle(String title, String description, String url, String source, 
                      LocalDateTime publishedAt, Double sentimentScore) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.source = source;
        this.publishedAt = publishedAt;
        this.sentimentScore = sentimentScore;
    }
}

