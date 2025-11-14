package com.cs203.tariffg4t2.controller.chatbot;

import com.cs203.tariffg4t2.dto.chatbot.ChatMessageRequest;
import com.cs203.tariffg4t2.dto.chatbot.ChatMessageResponse;
import com.cs203.tariffg4t2.service.chatbot.ConversationalChatbotService;
import com.cs203.tariffg4t2.service.chatbot.HsRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Controller for RAG-enabled conversational chatbot
 * Provides intelligent Q&A about tariffs, HS codes, and trade
 */
@RestController
@RequestMapping("/api/chat")
@Validated
@RequiredArgsConstructor
public class ChatbotController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatbotController.class);
    
    private final ConversationalChatbotService chatbotService;
    private final HsRateLimiter rateLimiter;
    
    /**
     * Send a message to the chatbot and get AI response with RAG context
     */
    @PostMapping("/message")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @Valid @RequestBody ChatMessageRequest request,
            HttpServletRequest httpServletRequest,
            @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor
    ) {
        logger.info("Chat message received: {} (session: {})", 
            request.getMessage().substring(0, Math.min(50, request.getMessage().length())),
            request.getSessionId());
        
        // Rate limiting
        String rateLimitKey = buildRateLimitKey(request, httpServletRequest, forwardedFor);
        if (!rateLimiter.tryConsume(rateLimitKey)) {
            logger.warn("Rate limit triggered for chat key={}", rateLimitKey);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        
        try {
            // Generate or use existing session ID
            String sessionId = request.getSessionId() != null 
                ? request.getSessionId() 
                : UUID.randomUUID().toString();
            
            // Process question with RAG
            String aiResponse = chatbotService.processQuestion(request.getMessage(), sessionId);
            
            // Build response
            ChatMessageResponse response = ChatMessageResponse.builder()
                .message(aiResponse)
                .sessionId(sessionId)
                .usedKnowledgeBase(true)  // Always true when using RAG
                .knowledgeEntriesUsed(5)   // We retrieve top 5
                .timestamp(Instant.now().toString())
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to process chat message: {}", e.getMessage(), e);
            
            // Return error response
            ChatMessageResponse errorResponse = ChatMessageResponse.builder()
                .message("I'm having trouble right now. Please try again in a moment, or contact support if the issue persists.")
                .sessionId(request.getSessionId())
                .usedKnowledgeBase(false)
                .timestamp(Instant.now().toString())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chatbot service is running");
    }
    
    private String buildRateLimitKey(ChatMessageRequest request,
                                     HttpServletRequest httpServletRequest,
                                     String forwardedFor) {
        if (request.getSessionId() != null) {
            return "chat:session:" + request.getSessionId();
        }
        
        String ipAddress = extractClientIp(forwardedFor, httpServletRequest);
        return "chat:ip:" + ipAddress;
    }
    
    private String extractClientIp(String forwardedFor, HttpServletRequest request) {
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
