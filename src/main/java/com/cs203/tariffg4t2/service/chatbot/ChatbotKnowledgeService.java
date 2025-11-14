package com.cs203.tariffg4t2.service.chatbot;

import com.cs203.tariffg4t2.model.chatbot.ChatbotKnowledge;
import com.cs203.tariffg4t2.repository.chatbot.ChatbotKnowledgeRepository;
import com.cs203.tariffg4t2.service.data.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing chatbot knowledge base with RAG
 * Handles population, embedding generation, and retrieval of knowledge entries
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChatbotKnowledgeService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatbotKnowledgeService.class);
    
    private final ChatbotKnowledgeRepository knowledgeRepository;
    private final EmbeddingService embeddingService;
    
    /**
     * Populate the knowledge base with initial content
     * This should be called once during application setup
     */
    public void populateKnowledgeBase() {
        logger.info("Starting knowledge base population...");
        
        long existingCount = knowledgeRepository.count();
        if (existingCount > 0) {
            logger.info("Knowledge base already has {} entries. Skipping population.", existingCount);
            return;
        }
        
        List<ChatbotKnowledge> allKnowledge = new ArrayList<>();
        
        // Add HS Code knowledge
        allKnowledge.addAll(createHsCodeKnowledge());
        
        // Add Tariff Guide knowledge
        allKnowledge.addAll(createTariffGuideKnowledge());
        
        // Add FAQ knowledge
        allKnowledge.addAll(createFaqKnowledge());
        
        // Add General knowledge
        allKnowledge.addAll(createGeneralKnowledge());
        
        // Save all entries (without embeddings first for speed)
        logger.info("Saving {} knowledge entries...", allKnowledge.size());
        List<ChatbotKnowledge> savedEntries = knowledgeRepository.saveAll(allKnowledge);
        
        // Generate embeddings asynchronously or in batch
        logger.info("Generating embeddings for {} entries...", savedEntries.size());
        generateEmbeddingsForEntries(savedEntries);
        
        logger.info("Knowledge base population completed. Total entries: {}", savedEntries.size());
    }
    
    /**
     * Generate embeddings for knowledge entries
     */
    public void generateEmbeddingsForEntries(List<ChatbotKnowledge> entries) {
        if (!embeddingService.isConfigured()) {
            logger.warn("Embedding service not configured. Skipping embedding generation.");
            return;
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (ChatbotKnowledge entry : entries) {
            try {
                float[] embedding = embeddingService.generateEmbedding(entry.getContent());
                String vectorString = embeddingService.embeddingToVectorString(embedding);
                entry.setEmbedding(vectorString);
                knowledgeRepository.save(entry);
                successCount++;
                
                if (successCount % 10 == 0) {
                    logger.info("Generated {} embeddings so far...", successCount);
                }
            } catch (Exception e) {
                logger.error("Failed to generate embedding for entry {}: {}", entry.getId(), e.getMessage());
                failCount++;
            }
        }
        
        logger.info("Embedding generation complete. Success: {}, Failed: {}", successCount, failCount);
    }
    
    /**
     * Retrieve relevant knowledge for a query using RAG
     */
    public List<ChatbotKnowledge> retrieveRelevantKnowledge(String query, int topK) throws Exception {
        if (!embeddingService.isConfigured()) {
            logger.warn("Embedding service not configured. Cannot perform semantic search.");
            return new ArrayList<>();
        }
        
        // Generate embedding for the query
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        String vectorString = embeddingService.embeddingToVectorString(queryEmbedding);
        
        // Find similar knowledge entries
        List<ChatbotKnowledge> results = knowledgeRepository.findSimilarKnowledge(vectorString, topK);
        
        logger.debug("Retrieved {} relevant knowledge entries for query: {}", results.size(), query);
        return results;
    }
    
    /**
     * Retrieve relevant knowledge filtered by category
     */
    public List<ChatbotKnowledge> retrieveRelevantKnowledgeByCategory(
            String query, String category, int topK) throws Exception {
        
        if (!embeddingService.isConfigured()) {
            return new ArrayList<>();
        }
        
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        String vectorString = embeddingService.embeddingToVectorString(queryEmbedding);
        
        return knowledgeRepository.findSimilarKnowledgeByCategory(vectorString, category, topK);
    }
    
    // ============================================================
    // KNOWLEDGE CONTENT CREATION METHODS
    // ============================================================
    
    private List<ChatbotKnowledge> createHsCodeKnowledge() {
        List<ChatbotKnowledge> knowledge = new ArrayList<>();
        
        knowledge.add(new ChatbotKnowledge(
            "HS Code (Harmonized System Code) is a standardized 6-10 digit code used internationally to classify traded products. " +
            "It's maintained by the World Customs Organization (WCO) and used by customs authorities worldwide. " +
            "The first 6 digits are standardized globally, while additional digits may vary by country.",
            "hs_code", "definition"
        ));
        
        knowledge.add(new ChatbotKnowledge(
            "To find an HS code: 1) Search by product name in the dropdown, 2) Be specific about the product material and function, " +
            "3) Visit https://www.trade.gov/harmonized-system-hs-codes for detailed lookup, " +
            "4) Consult the official customs tariff schedule of your country.",
            "hs_code", "how_to_find"
        ));
        
        knowledge.add(new ChatbotKnowledge(
            "HS Code structure: First 2 digits = Chapter (broad category like 'Live Animals'), " +
            "Next 2 digits = Heading (subcategory like 'Cattle'), " +
            "Next 2 digits = Subheading (specific type like 'Live cattle other than pure-bred breeding'), " +
            "Additional digits = Country-specific classifications. " +
            "Example: 010420 means Live goats.",
            "hs_code", "structure"
        ));
        
        knowledge.add(new ChatbotKnowledge(
            "Common HS code examples: Live cattle = 0102, Beef = 0201, Dairy products = 0401-0406, " +
            "Coffee = 0901, Tea = 0902, Wheat = 1001, Rice = 1006, Electronics start with 85, " +
            "Vehicles start with 87, Textiles start with 50-63.",
            "hs_code", "examples"
        ));
        
        return knowledge;
    }
    
    private List<ChatbotKnowledge> createTariffGuideKnowledge() {
        List<ChatbotKnowledge> knowledge = new ArrayList<>();
        
        knowledge.add(new ChatbotKnowledge(
            "Tariff calculation includes: 1) Product value (FOB - Free on Board), " +
            "2) Shipping cost (freight), 3) Insurance, 4) Tariff rate (varies by HS code and country), " +
            "5) VAT/GST (consumption tax), 6) Other duties if applicable. " +
            "Formula: Customs Value (CV) = Product Value + Freight + Insurance. " +
            "Total = CV + (CV × Tariff Rate) + (CV + Tariff) × VAT Rate.",
            "tariff_guide", "calculation"
        ));
        
        knowledge.add(new ChatbotKnowledge(
            "Required fields for tariff calculation: Importing country (2-letter code like US, SG), " +
            "Exporting country, HS code (6-10 digits), Product value in USD, " +
            "Optional: Shipping mode (SEA/AIR), Weight, Number of items, Freight cost, Insurance cost.",
            "tariff_guide", "required_fields"
        ));
        
        knowledge.add(new ChatbotKnowledge(
            "Shipping modes affect costs: SEA (ocean freight) is cheaper but slower, typically for bulk goods. " +
            "AIR is faster but more expensive, used for perishables or urgent items. " +
            "Shipping rates vary by weight, volume, and route. Our system provides estimates based on current rates.",
            "tariff_guide", "shipping"
        ));
        
        knowledge.add(new ChatbotKnowledge(
            "VAT and GST: VAT (Value Added Tax) is applied in most countries on imports. " +
            "GST (Goods and Services Tax) is similar, used in countries like Singapore, Australia, India. " +
            "Rates vary by country: EU countries 15-27%, Singapore 8-9%, Australia 10%, USA has sales tax by state. " +
            "Some products may be exempt or zero-rated.",
            "tariff_guide", "vat_gst"
        ));
        
        knowledge.add(new ChatbotKnowledge(
            "Free Trade Agreements (FTAs) can reduce or eliminate tariffs. Common FTAs: " +
            "USMCA (US-Mexico-Canada), ASEAN FTA, EU Single Market, RCEP (Asia-Pacific), " +
            "CPTPP (Trans-Pacific Partnership). Check if your trade route has an FTA for potential savings. " +
            "Our system accounts for applicable FTA rates when available.",
            "tariff_guide", "fta"
        ));
        
        return knowledge;
    }
    
    private List<ChatbotKnowledge> createFaqKnowledge() {
        List<ChatbotKnowledge> knowledge = new ArrayList<>();
        
        knowledge.add(new ChatbotKnowledge(
            "Q: What if I don't know the exact HS code? " +
            "A: Start by searching our product dropdown with keywords. You can also describe your product and I'll help narrow it down. " +
            "For official verification, visit your country's customs website or consult a customs broker.",
            "faq", "hs_code_unknown"
        ));
        
        knowledge.add(new ChatbotKnowledge(
            "Q: Why is my tariff calculation different from expectations? " +
            "A: Tariff rates change frequently based on trade policies, FTAs, and diplomatic relations. " +
            "Our system uses the latest available rates, but always verify with official customs for final accuracy. " +
            "Exchange rates also fluctuate daily, affecting USD-based calculations.",
            "faq", "calculation_difference"
        ));
        
        knowledge.add(new ChatbotKnowledge(
            "Q: What currency should I use for product value? " +
            "A: Enter product value in USD. Our system will convert to local currency using current exchange rates. " +
            "If your invoice is in another currency, convert to USD first using the exchange rate on the invoice date.",
            "faq", "currency"
        ));
        
        knowledge.add(new ChatbotKnowledge(
            "Q: Do I need to include shipping in product value? " +
            "A: No, enter product value separately from shipping. Our system calculates customs value by adding " +
            "product value + freight + insurance. This is the CIF (Cost, Insurance, Freight) value used for tariff calculation.",
            "faq", "shipping_in_value"
        ));
        
        knowledge.add(new ChatbotKnowledge(
            "Q: How accurate is the predictive analysis? " +
            "A: Our predictive analysis uses AI to analyze news sentiment, exchange rate trends, and historical data. " +
            "It provides guidance (BUY/WAIT/HOLD) but should not be the sole basis for business decisions. " +
            "Consider it as one input among market research, supplier relationships, and business needs.",
            "faq", "prediction_accuracy"
        ));
        
        knowledge.add(new ChatbotKnowledge(
            "Q: What is the difference between tariff and VAT/GST? " +
            "A: Tariff (customs duty) is a tax on imports, specific to each product category and trade route. " +
            "VAT/GST is a consumption tax applied to most goods and services in a country, whether imported or domestic. " +
            "You typically pay both on imports: tariff first, then VAT on the (product + tariff) total.",
            "faq", "tariff_vs_vat"
        ));
        
        return knowledge;
    }
    
    private List<ChatbotKnowledge> createGeneralKnowledge() {
        List<ChatbotKnowledge> knowledge = new ArrayList<>();
        
        knowledge.add(new ChatbotKnowledge(
            "TariffNom is a comprehensive tariff calculation and trade intelligence platform. " +
            "Features include: Real-time tariff calculation, HS code lookup, Predictive market analysis, " +
            "Exchange rate tracking, News sentiment analysis, Multi-country support, Historical data comparison.",
            "general", "about"
        ));
        
        knowledge.add(new ChatbotKnowledge(
            "To calculate tariffs: 1) Go to the main calculator page, 2) Select importing and exporting countries, " +
            "3) Enter or search for HS code, 4) Input product value, 5) Optionally add shipping details, " +
            "6) Click Calculate to see breakdown of all costs including tariff, VAT, and total.",
            "general", "how_to_use"
        ));
        
        knowledge.add(new ChatbotKnowledge(
            "For trade predictions: Use the Predictive Analysis feature to get BUY/WAIT/HOLD recommendations " +
            "based on current market conditions, news sentiment about trade relations, and exchange rate trends. " +
            "This helps time your purchases for better costs.",
            "general", "predictions"
        ));
        
        knowledge.add(new ChatbotKnowledge(
            "Data sources: Tariff rates from official customs databases, Exchange rates from ECB and central banks, " +
            "News from NewsAPI covering global trade, Shipping rates from industry standard calculators. " +
            "All data is regularly updated to maintain accuracy.",
            "general", "data_sources"
        ));
        
        return knowledge;
    }
}
