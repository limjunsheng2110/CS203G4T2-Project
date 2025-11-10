package com.cs203.tariffg4t2.service.data;

import com.cs203.tariffg4t2.model.basic.NewsArticle;
import com.cs203.tariffg4t2.repository.basic.NewsArticleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsAPIService {
    
    private static final Logger logger = LoggerFactory.getLogger(NewsAPIService.class);
    private static final String[] TRADE_KEYWORDS = {
        "tariff", "imports", "exports", "currency", "trade policy", 
        "trade war", "customs", "international trade"
    };
    
    private final NewsArticleRepository newsArticleRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${newsapi.api.key:}")
    private String apiKey;
    
    @Value("${newsapi.api.url:https://newsapi.org/v2}")
    private String apiUrl;
    
    /**
     * Fetch recent trade and economic news articles from News API
     * @param daysBack Number of days to look back
     * @return List of articles (not yet saved to database)
     */
    public List<NewsArticle> fetchTradeNews(int daysBack) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("News API key not configured");
        }
        
        LocalDateTime fromDate = LocalDateTime.now().minusDays(daysBack);
        String fromDateStr = fromDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        // Simplified query - just use "trade" instead of OR combination
        String query = "trade";
        
        // News API endpoint: /everything
        String url = String.format("%s/everything?q=%s&from=%s&language=en&sortBy=publishedAt&pageSize=100&apiKey=%s",
                                   apiUrl, 
                                   query,
                                   fromDateStr,
                                   apiKey);
        
        logger.info("Fetching trade news from News API (past {} days)", daysBack);
        logger.info("Query: '{}', From date: {}", query, fromDateStr);
        logger.debug("API URL: {}", url.replace(apiKey, "***"));
        
        try {
            String response = restTemplate.getForObject(url, String.class);
            logger.info("DEBUG: Got response from News API, length: {} chars", response != null ? response.length() : 0);
            
            List<NewsArticle> articles = parseNewsArticles(response);
            logger.info("DEBUG: Parsed {} articles successfully", articles.size());
            
            return articles;
        } catch (Exception e) {
            logger.error("Failed to fetch news from API: {}", e.getMessage(), e);
            throw new Exception("News API request failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse JSON response from News API
     */
    private List<NewsArticle> parseNewsArticles(String jsonResponse) throws Exception {
        List<NewsArticle> articles = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            
            // Check API response status
            String status = root.has("status") ? root.get("status").asText() : "unknown";
            logger.info("DEBUG: News API response status: {}", status);
            
            if (!"ok".equals(status)) {
                String message = root.has("message") ? root.get("message").asText() : "Unknown error";
                String code = root.has("code") ? root.get("code").asText() : "unknown";
                throw new Exception("News API error [" + code + "]: " + message);
            }
            
            // Check total results
            int totalResults = root.has("totalResults") ? root.get("totalResults").asInt() : 0;
            logger.info("DEBUG: News API returned totalResults: {}", totalResults);
            
            // Parse articles array
            JsonNode articlesNode = root.get("articles");
            if (articlesNode == null || !articlesNode.isArray()) {
                logger.warn("No articles array found in API response");
                logger.debug("DEBUG: Response structure: {}", root.toPrettyString().substring(0, Math.min(500, root.toPrettyString().length())));
                return articles;
            }
            
            logger.info("DEBUG: Articles array has {} items", articlesNode.size());
            
            int parsedCount = 0;
            int duplicateCount = 0;
            
            for (JsonNode articleNode : articlesNode) {
                try {
                    NewsArticle article = parseArticle(articleNode);
                    
                    // Avoid duplicates
                    if (article.getUrl() != null && !newsArticleRepository.existsByUrl(article.getUrl())) {
                        articles.add(article);
                        parsedCount++;
                    } else {
                        duplicateCount++;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse article: {}", e.getMessage());
                }
            }
            
            logger.info("Successfully parsed {} new articles ({} duplicates skipped)", parsedCount, duplicateCount);
            return articles;
            
        } catch (Exception e) {
            logger.error("Error parsing News API response: {}", e.getMessage());
            throw new Exception("Failed to parse news articles: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse individual article from JSON
     */
    private NewsArticle parseArticle(JsonNode articleNode) {
        NewsArticle article = new NewsArticle();
        
        // Title
        if (articleNode.has("title") && !articleNode.get("title").isNull()) {
            article.setTitle(articleNode.get("title").asText());
        }
        
        // Description
        if (articleNode.has("description") && !articleNode.get("description").isNull()) {
            article.setDescription(articleNode.get("description").asText());
        }
        
        // URL
        if (articleNode.has("url") && !articleNode.get("url").isNull()) {
            article.setUrl(articleNode.get("url").asText());
        }
        
        // Source
        if (articleNode.has("source") && articleNode.get("source").has("name")) {
            article.setSource(articleNode.get("source").get("name").asText());
        }
        
        // Published date
        if (articleNode.has("publishedAt") && !articleNode.get("publishedAt").isNull()) {
            String publishedAtStr = articleNode.get("publishedAt").asText();
            try {
                ZonedDateTime zdt = ZonedDateTime.parse(publishedAtStr);
                article.setPublishedAt(zdt.toLocalDateTime());
            } catch (Exception e) {
                logger.warn("Failed to parse date: {}", publishedAtStr);
                article.setPublishedAt(LocalDateTime.now());
            }
        }
        
        // Identify matched keywords
        String content = (article.getTitle() + " " + article.getDescription()).toLowerCase();
        StringBuilder matchedKeywords = new StringBuilder();
        for (String keyword : TRADE_KEYWORDS) {
            if (content.contains(keyword.toLowerCase())) {
                if (matchedKeywords.length() > 0) matchedKeywords.append(", ");
                matchedKeywords.append(keyword);
            }
        }
        article.setKeywords(matchedKeywords.toString());
        
        return article;
    }
    
    /**
     * Test API connectivity
     */
    public boolean testAPIConnection() {
        try {
            String url = String.format("%s/top-headlines?country=us&category=business&apiKey=%s",
                                     apiUrl, apiKey);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            return "ok".equals(root.get("status").asText());
        } catch (Exception e) {
            logger.error("News API connection test failed: {}", e.getMessage());
            return false;
        }
    }
}

