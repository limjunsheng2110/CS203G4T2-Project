package com.cs203.tariffg4t2.dto.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for conversational chatbot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    
    /**
     * The AI's response message
     */
    private String message;
    
    /**
     * Session ID for tracking conversation
     */
    private String sessionId;
    
    /**
     * Indicates if response used RAG context
     */
    private Boolean usedKnowledgeBase;
    
    /**
     * Number of knowledge entries used for context
     */
    private Integer knowledgeEntriesUsed;
    
    /**
     * Timestamp of the response
     */
    private String timestamp;
}
