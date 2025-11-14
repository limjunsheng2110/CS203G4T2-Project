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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final EmbeddingService embeddingService;
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
        return fetchTradeNews(daysBack, null, null);
    }

    /**
     * Fetch trade news with optional country filters
     * @param daysBack Number of days to look back
     * @param country1 First country name (optional)
     * @param country2 Second country name (optional)
     * @return List of articles
     */
    public List<NewsArticle> fetchTradeNews(int daysBack, String country1, String country2) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("News API key not configured");
        }
        
        LocalDateTime fromDate = LocalDateTime.now().minusDays(daysBack);
        String fromDateStr = fromDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        // Build query with country-specific terms if provided
        String query;
        if (country1 != null && country2 != null) {
            // Expand country names to include variations
            String expandedCountry1 = expandCountryName(country1);
            String expandedCountry2 = expandCountryName(country2);

            // Less strict query - OR between countries, but require trade keywords
            // This casts a wider net, then we filter on the backend
            query = String.format("(%s OR %s) AND (trade OR tariff OR import OR export OR customs OR economy OR \"trade war\" OR \"trade deal\" OR \"trade relations\" OR \"trade policy\")",
                expandedCountry1, expandedCountry2);
            logger.info("Fetching country-relevant trade news for: {} and {}", country1, country2);
        } else {
            // General trade-related query
            String[] keywords = {
                "trade policy",
                "trade tariff",
                "customs duty",
                "import export",
                "supply chain"
            };
            query = String.join(" OR ", keywords);
            logger.info("Fetching general trade news");
        }

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        
        // News API endpoint: /everything
        String url = String.format("%s/everything?q=%s&from=%s&sortBy=publishedAt&pageSize=100&language=en&apiKey=%s",
                                   apiUrl,
                                   encodedQuery,
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
     * Expand country name to include common variations
     * This helps match more articles in the News API
     */
    private String expandCountryName(String country) {
        String upper = country.toUpperCase();

        // Map common abbreviations and variations
        switch (upper) {
            case "US":
            case "USA":
                return "\"United States\" OR USA OR US OR America OR American";
            case "CN":
            case "CHINA":
                return "China OR Chinese OR PRC";
            case "UK":
            case "GB":
                return "\"United Kingdom\" OR UK OR Britain OR British";
            case "EU":
                return "\"European Union\" OR EU OR Europe OR European";
            case "JP":
            case "JAPAN":
                return "Japan OR Japanese";
            case "KR":
            case "KOREA":
                return "\"South Korea\" OR Korea OR Korean";
            case "IN":
            case "INDIA":
                return "India OR Indian";
            case "MX":
            case "MEXICO":
                return "Mexico OR Mexican";
            case "CA":
            case "CANADA":
                return "Canada OR Canadian";
            case "BR":
            case "BRAZIL":
                return "Brazil OR Brazilian";
            case "DE":
            case "GERMANY":
                return "Germany OR German";
            case "FR":
            case "FRANCE":
                return "France OR French";
            default:
                // For other countries, return as-is with both full name and code
                return String.format("%s OR %s", country, upper);
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
            int embeddingCount = 0;
            
            for (JsonNode articleNode : articlesNode) {
                try {
                    NewsArticle article = parseArticle(articleNode);
                    
                    // Avoid duplicates
                    if (article.getUrl() != null && !newsArticleRepository.existsByUrl(article.getUrl())) {
                        // Generate embedding for RAG semantic search
                        if (embeddingService.isConfigured()) {
                            try {
                                String contentToEmbed = article.getTitle() + " " + 
                                    (article.getDescription() != null ? article.getDescription() : "");
                                
                                float[] embedding = embeddingService.generateEmbedding(contentToEmbed);
                                String vectorString = embeddingService.embeddingToVectorString(embedding);
                                article.setEmbedding(vectorString);
                                embeddingCount++;
                                
                                logger.debug("Generated embedding for article: {}", article.getTitle());
                            } catch (Exception e) {
                                logger.warn("Failed to generate embedding for article '{}': {}", 
                                    article.getTitle(), e.getMessage());
                                // Continue without embedding - article will still be saved
                            }
                        }
                        
                        articles.add(article);
                        parsedCount++;
                    } else {
                        duplicateCount++;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse article: {}", e.getMessage());
                }
            }
            
            logger.info("Successfully parsed {} new articles ({} duplicates skipped, {} embeddings generated)", 
                parsedCount, duplicateCount, embeddingCount);
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
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public boolean testAPIConnection() throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("News API key not configured");
        }

        String url = String.format("%s/top-headlines?country=us&category=business&apiKey=%s",
                                 apiUrl, apiKey);
        logger.info("Testing News API connectivity with URL: {}", url.replace(apiKey, "***"));

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            String status = root.has("status") ? root.get("status").asText() : "unknown";

            if (!"ok".equals(status)) {
                String message = root.has("message") ? root.get("message").asText() : "Unknown error";
                String code = root.has("code") ? root.get("code").asText() : "unknown";
                throw new Exception("News API responded with status '" + status + "' (" + code + "): " + message);
            }

            logger.info("News API connectivity test succeeded");
            return true;
        } catch (Exception e) {
            logger.error("News API connection test failed: {}", e.getMessage(), e);
            throw e;
        }
    }
}
