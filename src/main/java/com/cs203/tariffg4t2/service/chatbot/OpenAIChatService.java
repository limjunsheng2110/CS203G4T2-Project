package com.cs203.tariffg4t2.service.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for communicating with OpenAI Chat API
 * Provides conversational AI capabilities for the chatbot
 */
@Service
@RequiredArgsConstructor
public class OpenAIChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIChatService.class);
    
    @Value("${openai.api.key}")
    private String openAiApiKey;
    
    @Value("${openai.chat.url:https://api.openai.com/v1/chat/completions}")
    private String chatUrl;
    
    @Value("${openai.chat.model:gpt-4o-mini}")
    private String chatModel;
    
    @Value("${openai.chat.max-tokens:800}")
    private int maxTokens;
    
    @Value("${openai.chat.temperature:0.7}")
    private double temperature;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Send a chat message to OpenAI with optional context
     * 
     * @param userMessage The user's question/message
     * @param systemPrompt System instructions for the AI
     * @param context Additional context to help answer the question (from RAG)
     * @return AI's response
     */
    public String chat(String userMessage, String systemPrompt, String context) throws Exception {
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("User message cannot be empty");
        }
        
        List<Map<String, String>> messages = new ArrayList<>();
        
        // System message with instructions
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        
        // Add context from RAG if available
        if (context != null && !context.isBlank()) {
            messages.add(Map.of(
                "role", "system", 
                "content", "Relevant information from knowledge base:\n\n" + context
            ));
        }
        
        // User message
        messages.add(Map.of("role", "user", "content", userMessage));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);
        
        Map<String, Object> requestBody = Map.of(
            "model", chatModel,
            "messages", messages,
            "max_tokens", maxTokens,
            "temperature", temperature
        );
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            logger.debug("Sending chat request to OpenAI. Model: {}, Tokens: {}", chatModel, maxTokens);
            
            ResponseEntity<String> response = restTemplate.postForEntity(chatUrl, request, String.class);
            
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new Exception("OpenAI API returned status: " + response.getStatusCode());
            }
            
            // Parse response
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.path("choices");
            
            if (choices.isEmpty()) {
                throw new Exception("No response from OpenAI");
            }
            
            String aiResponse = choices.get(0)
                .path("message")
                .path("content")
                .asText();
            
            logger.debug("Received response from OpenAI. Length: {} chars", aiResponse.length());
            return aiResponse;
            
        } catch (Exception e) {
            logger.error("Failed to get chat response from OpenAI: {}", e.getMessage(), e);
            throw new Exception("Chat request failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Simple chat without context (for basic conversations)
     */
    public String chat(String userMessage, String systemPrompt) throws Exception {
        return chat(userMessage, systemPrompt, null);
    }
    
    /**
     * Check if chat service is configured
     */
    public boolean isConfigured() {
        return openAiApiKey != null && !openAiApiKey.isBlank();
    }
}
