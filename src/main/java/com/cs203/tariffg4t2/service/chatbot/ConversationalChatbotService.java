package com.cs203.tariffg4t2.service.chatbot;

import com.cs203.tariffg4t2.model.chatbot.ChatbotKnowledge;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG-enabled conversational chatbot service
 * Combines knowledge base retrieval with LLM for intelligent responses
 */
@Service
@RequiredArgsConstructor
public class ConversationalChatbotService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConversationalChatbotService.class);
    
    private final ChatbotKnowledgeService knowledgeService;
    private final OpenAIChatService openAIChatService;
    private final HsResolverService hsResolverService;
    
    private static final String SYSTEM_PROMPT = """
        You are TariffNom's helpful assistant, an expert in international trade, tariffs, and HS codes.
        Your role is to help users:
        - Understand HS codes and how to find them
        - Calculate tariffs and understand the calculation process
        - Navigate international trade regulations
        - Use the TariffNom platform effectively
        
        Guidelines:
        - Be concise and clear (2-3 paragraphs maximum)
        - Use bullet points for lists
        - Provide specific examples when helpful
        - If you don't know something, suggest where to find official information
        - Always use the context provided from the knowledge base
        - For HS code lookups, explain that users can search the product dropdown or describe their product
        
        Format:
        - Use markdown for formatting
        - Keep responses friendly but professional
        - Include relevant examples from the context when available
        """;
    
    /**
     * Process a user question with RAG
     * 
     * @param userQuestion The user's question
     * @param sessionId Session identifier (optional)
     * @return AI-generated response with context from knowledge base
     */
    public String processQuestion(String userQuestion, String sessionId) throws Exception {
        if (userQuestion == null || userQuestion.isBlank()) {
            throw new IllegalArgumentException("Question cannot be empty");
        }
        
        logger.info("Processing question: {} (session: {})", 
            userQuestion.substring(0, Math.min(50, userQuestion.length())), sessionId);
        
        // Check if OpenAI is configured
        if (!openAIChatService.isConfigured()) {
            logger.warn("OpenAI not configured, returning fallback response");
            return getFallbackResponse(userQuestion);
        }
        
        try {
            // Step 1: Retrieve relevant knowledge using RAG
            String context = retrieveRelevantContext(userQuestion);
            
            // Step 2: Generate response using LLM with context
            String aiResponse = openAIChatService.chat(userQuestion, SYSTEM_PROMPT, context);
            
            logger.info("Successfully generated response. Length: {} chars", aiResponse.length());
            return aiResponse;
            
        } catch (Exception e) {
            logger.error("Failed to process question with RAG: {}", e.getMessage(), e);
            // Fallback to simple response
            return getFallbackResponse(userQuestion);
        }
    }
    
    /**
     * Retrieve relevant context from knowledge base using RAG
     */
    private String retrieveRelevantContext(String query) {
        try {
            // Retrieve top 5 most relevant knowledge entries
            List<ChatbotKnowledge> relevantKnowledge = 
                knowledgeService.retrieveRelevantKnowledge(query, 5);
            
            if (relevantKnowledge.isEmpty()) {
                logger.warn("No relevant knowledge found for query");
                return "";
            }
            
            logger.info("Retrieved {} relevant knowledge entries", relevantKnowledge.size());
            
            // Format knowledge into context string
            String context = relevantKnowledge.stream()
                .map(knowledge -> {
                    String categoryLabel = formatCategoryLabel(knowledge.getCategory());
                    return String.format("[%s] %s", categoryLabel, knowledge.getContent());
                })
                .collect(Collectors.joining("\n\n"));
            
            return context;
            
        } catch (Exception e) {
            logger.error("Failed to retrieve context: {}", e.getMessage());
            return "";
        }
    }
    
    /**
     * Detect if question is asking for HS code lookup
     */
    private boolean isHsCodeLookupQuestion(String question) {
        String lower = question.toLowerCase();
        return lower.contains("hs code") && 
               (lower.contains("what is the") || 
                lower.contains("find") || 
                lower.contains("lookup") ||
                lower.contains("code for"));
    }
    
    /**
     * Format category label for better readability
     */
    private String formatCategoryLabel(String category) {
        if (category == null) return "Info";
        return switch (category.toLowerCase()) {
            case "hs_code" -> "HS Code";
            case "tariff_guide" -> "Tariff Guide";
            case "faq" -> "FAQ";
            case "general" -> "General";
            default -> category;
        };
    }
    
    /**
     * Fallback response when AI is not available
     */
    private String getFallbackResponse(String question) {
        String lower = question.toLowerCase();
        
        if (lower.contains("hs code") && lower.contains("what is")) {
            return "An **HS Code** (Harmonized System Code) is a standardized international code used to classify traded products. " +
                   "It's a 6-10 digit number where the first 6 digits are globally standardized.\n\n" +
                   "To find an HS code:\n" +
                   "- Search by product name in our dropdown\n" +
                   "- Describe your product with specific details\n" +
                   "- Visit https://www.trade.gov/harmonized-system-hs-codes\n\n" +
                   "Example: Live goats = 010420";
        }
        
        if (lower.contains("tariff") && lower.contains("calculate")) {
            return "To calculate tariffs:\n\n" +
                   "1. Select importing and exporting countries\n" +
                   "2. Enter or search for the HS code\n" +
                   "3. Input product value in USD\n" +
                   "4. Add shipping details (optional)\n" +
                   "5. Click Calculate\n\n" +
                   "The system will show:\n" +
                   "- Customs value (product + freight + insurance)\n" +
                   "- Tariff amount\n" +
                   "- VAT/GST\n" +
                   "- Total cost";
        }
        
        return "I'm here to help with tariff calculations and HS codes! " +
               "I can explain how to use TariffNom, find HS codes, or understand tariff calculations. " +
               "What would you like to know?";
    }
}
