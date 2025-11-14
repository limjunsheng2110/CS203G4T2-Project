package com.cs203.tariffg4t2.dto.chatbot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for conversational chatbot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    
    /**
     * The user's question or message
     */
    @NotBlank(message = "Message cannot be empty")
    @Size(max = 2000, message = "Message too long (max 2000 characters)")
    private String message;
    
    /**
     * Session ID for conversation tracking (optional)
     */
    private String sessionId;
    
    /**
     * User consent for logging (GDPR compliance)
     */
    private Boolean consentLogging;
}
