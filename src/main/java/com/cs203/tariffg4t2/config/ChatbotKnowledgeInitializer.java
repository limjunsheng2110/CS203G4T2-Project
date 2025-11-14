package com.cs203.tariffg4t2.config;

import com.cs203.tariffg4t2.service.chatbot.ChatbotKnowledgeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Configuration to initialize chatbot knowledge base on application startup
 * Runs after database schema is created
 */
@Configuration
@RequiredArgsConstructor
public class ChatbotKnowledgeInitializer {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatbotKnowledgeInitializer.class);
    
    private final ChatbotKnowledgeService chatbotKnowledgeService;
    
    /**
     * Populate knowledge base on startup
     * Runs after database is ready
     */
    @Bean
    @Order(100) // Run after other initializers
    public CommandLineRunner initializeKnowledgeBase() {
        return args -> {
            try {
                logger.info("=".repeat(60));
                logger.info("Initializing Chatbot Knowledge Base with RAG...");
                logger.info("=".repeat(60));
                
                // Populate knowledge base (will skip if already populated)
                chatbotKnowledgeService.populateKnowledgeBase();
                
                logger.info("=".repeat(60));
                logger.info("Chatbot Knowledge Base initialization complete!");
                logger.info("=".repeat(60));
                
            } catch (Exception e) {
                logger.error("Failed to initialize chatbot knowledge base: {}", e.getMessage(), e);
                logger.warn("Chatbot will continue to work but RAG may not be available");
            }
        };
    }
}
