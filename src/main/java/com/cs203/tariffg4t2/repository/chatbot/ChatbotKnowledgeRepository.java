package com.cs203.tariffg4t2.repository.chatbot;

import com.cs203.tariffg4t2.model.chatbot.ChatbotKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ChatbotKnowledge with RAG vector similarity search
 */
@Repository
public interface ChatbotKnowledgeRepository extends JpaRepository<ChatbotKnowledge, Long> {
    
    /**
     * Find knowledge entries by category
     */
    List<ChatbotKnowledge> findByCategory(String category);
    
    /**
     * Find knowledge entries by category and subcategory
     */
    List<ChatbotKnowledge> findByCategoryAndSubcategory(String category, String subcategory);
    
    /**
     * RAG: Find similar knowledge entries using cosine similarity on embeddings
     * Returns top K most similar entries based on semantic meaning
     * 
     * @param queryEmbedding The query embedding in vector format '[0.1, 0.2, ...]'
     * @param limit Maximum number of results to return
     * @return List of knowledge entries ordered by similarity (most similar first)
     */
    @Query(value = """
        SELECT *, 1 - (embedding <=> CAST(:queryEmbedding AS vector)) AS similarity
        FROM chatbot_knowledge
        WHERE embedding IS NOT NULL
        ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<ChatbotKnowledge> findSimilarKnowledge(
        @Param("queryEmbedding") String queryEmbedding,
        @Param("limit") int limit
    );
    
    /**
     * RAG with category filter: Find similar knowledge entries within a specific category
     * Useful when you want semantically similar content but only from certain categories
     * 
     * @param queryEmbedding The query embedding in vector format
     * @param category Category to filter by (e.g., "hs_code", "tariff_guide", "faq")
     * @param limit Maximum number of results
     * @return List of knowledge entries in the category, ordered by similarity
     */
    @Query(value = """
        SELECT *, 1 - (embedding <=> CAST(:queryEmbedding AS vector)) AS similarity
        FROM chatbot_knowledge
        WHERE embedding IS NOT NULL
          AND category = :category
        ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<ChatbotKnowledge> findSimilarKnowledgeByCategory(
        @Param("queryEmbedding") String queryEmbedding,
        @Param("category") String category,
        @Param("limit") int limit
    );
    
    /**
     * RAG with multiple categories: Find similar knowledge from specific categories
     * 
     * @param queryEmbedding The query embedding in vector format
     * @param categories List of categories to include
     * @param limit Maximum number of results
     * @return List of knowledge entries from specified categories, ordered by similarity
     */
    @Query(value = """
        SELECT *, 1 - (embedding <=> CAST(:queryEmbedding AS vector)) AS similarity
        FROM chatbot_knowledge
        WHERE embedding IS NOT NULL
          AND category IN (:categories)
        ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<ChatbotKnowledge> findSimilarKnowledgeByCategories(
        @Param("queryEmbedding") String queryEmbedding,
        @Param("categories") List<String> categories,
        @Param("limit") int limit
    );
    
    /**
     * Check if knowledge base has any entries with embeddings
     */
    @Query("SELECT COUNT(k) > 0 FROM ChatbotKnowledge k WHERE k.embedding IS NOT NULL")
    boolean hasEmbeddings();
    
    /**
     * Count knowledge entries by category
     */
    long countByCategory(String category);
    
    /**
     * Get all entries without embeddings (for migration/population)
     */
    @Query("SELECT k FROM ChatbotKnowledge k WHERE k.embedding IS NULL")
    List<ChatbotKnowledge> findEntriesWithoutEmbeddings();
    
    /**
     * Delete knowledge entries by category
     */
    void deleteByCategory(String category);
}
