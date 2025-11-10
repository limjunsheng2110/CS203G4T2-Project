-- Seed test data for predictive analysis testing
-- Run this to populate initial sentiment data

-- Insert test news articles
INSERT INTO news_article (title, description, url, source, published_at, sentiment_score, keywords, created_at)
VALUES 
    ('Trade agreement boosts cooperation between nations', 
     'New trade deal signals positive outlook for international commerce', 
     'https://example.com/news1', 'Reuters', 
     NOW() - INTERVAL '1 day', 0.65, 'trade, agreement, cooperation', NOW()),
    
    ('Currency markets stabilize amid trade talks', 
     'Exchange rates show signs of stability as negotiations continue', 
     'https://example.com/news2', 'Bloomberg', 
     NOW() - INTERVAL '2 days', 0.38, 'currency, trade', NOW()),
    
    ('Import tariffs cause concern in manufacturing sector', 
     'New tariff policies raise questions about supply chain costs', 
     'https://example.com/news3', 'Financial Times', 
     NOW() - INTERVAL '3 days', -0.42, 'tariff, imports', NOW()),
    
    ('Export growth shows positive trend in Q3', 
     'Trade data indicates strengthening international commerce', 
     'https://example.com/news4', 'Wall Street Journal', 
     NOW() - INTERVAL '4 days', 0.55, 'exports, growth, trade', NOW()),
    
    ('Trade policy uncertainty impacts business planning', 
     'Companies face challenges with unpredictable trade regulations', 
     'https://example.com/news5', 'CNBC', 
     NOW() - INTERVAL '5 days', -0.28, 'trade policy, uncertainty', NOW());

-- Insert test weekly sentiment analysis
INSERT INTO sentiment_analysis (week_start_date, week_end_date, average_sentiment, 
                                article_count, positive_count, negative_count, neutral_count, 
                                trend, created_at, updated_at)
VALUES 
    -- This week (most recent)
    (CURRENT_DATE - INTERVAL '6 days', CURRENT_DATE, 0.32, 25, 15, 7, 3, 'improving', NOW(), NOW()),
    
    -- Last week
    (CURRENT_DATE - INTERVAL '13 days', CURRENT_DATE - INTERVAL '7 days', 0.18, 28, 12, 10, 6, 'stable', NOW(), NOW()),
    
    -- 2 weeks ago
    (CURRENT_DATE - INTERVAL '20 days', CURRENT_DATE - INTERVAL '14 days', 0.15, 22, 10, 8, 4, 'declining', NOW(), NOW()),
    
    -- 3 weeks ago
    (CURRENT_DATE - INTERVAL '27 days', CURRENT_DATE - INTERVAL '21 days', 0.28, 30, 18, 8, 4, 'improving', NOW(), NOW());

-- Verify the data
SELECT 'News Articles:' as info, COUNT(*) as count FROM news_article
UNION ALL
SELECT 'Sentiment Analysis:' as info, COUNT(*) as count FROM sentiment_analysis;

-- Show recent sentiment
SELECT week_start_date, week_end_date, average_sentiment, trend, article_count 
FROM sentiment_analysis 
ORDER BY week_end_date DESC;

