package com.cs203.tariffg4t2.model.chatbot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "hs_chat_session")
public class HsChatSessionLog {

    @Id
    @Column(name = "session_id", length = 64, nullable = false)
    private String sessionId;

    @Column(name = "consent_logging")
    private Boolean consentLogging;

    @Column(name = "request_count")
    private Integer requestCount;

    @Column(name = "last_product_name", length = 150)
    private String lastProductName;

    @Column(name = "last_hs_code", length = 10)
    private String lastHsCode;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

