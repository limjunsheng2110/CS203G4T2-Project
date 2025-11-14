package com.cs203.tariffg4t2.service.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Service for generating text embeddings using OpenAI's Embedding API
 * These embeddings enable semantic search (RAG) in the PostgreSQL database
 */
@Service
@RequiredArgsConstructor
public class EmbeddingService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    
    @Value("${openai.api.key}")
    private String openAiApiKey;
    
    @Value("${openai.embedding.url:https://api.openai.com/v1/embeddings}")
    private String embeddingUrl;
    
    @Value("${openai.embedding.model:text-embedding-3-small}")
    private String embeddingModel;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Generate embedding vector for text using OpenAI API
     * @param text The text to embed
     * @return float array representing the embedding (1536 dimensions for text-embedding-3-small)
     */
    public float[] generateEmbedding(String text) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }
        
        // Truncate if too long (OpenAI has token limits)
        String truncatedText = text.length() > 8000 ? text.substring(0, 8000) : text;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);
        
        Map<String, Object> requestBody = Map.of(
            "model", embeddingModel,
            "input", truncatedText
        );
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            logger.debug("Generating embedding for text of length: {}", truncatedText.length());
            ResponseEntity<String> response = restTemplate.postForEntity(embeddingUrl, request, String.class);
            
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new Exception("OpenAI API returned status: " + response.getStatusCode());
            }
            
            // Parse response
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode embeddingArray = root.path("data").get(0).path("embedding");
            
            if (embeddingArray == null || !embeddingArray.isArray()) {
                throw new Exception("Invalid embedding response from OpenAI");
            }
            
            // Convert to float array
            float[] embedding = new float[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                embedding[i] = (float) embeddingArray.get(i).asDouble();
            }
            
            logger.debug("Generated embedding with {} dimensions", embedding.length);
            return embedding;
            
        } catch (Exception e) {
            logger.error("Failed to generate embedding: {}", e.getMessage(), e);
            throw new Exception("Embedding generation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Convert float array to PostgreSQL vector format: '[0.1, 0.2, ...]'
     */
    public String embeddingToVectorString(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        
        return sb.toString();
    }
    
    /**
     * Check if embedding service is configured
     */
    public boolean isConfigured() {
        return openAiApiKey != null && !openAiApiKey.isBlank();
    }
}
