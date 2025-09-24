package com.cs205.tariffg4t2.model.api;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScrapingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_url_id")
    private TargetUrl targetUrl;

    private String status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer recordsExtracted;

    private String errorMessage;
}