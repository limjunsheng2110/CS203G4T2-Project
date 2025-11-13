-- Seed exchange rate data for testing predictive analysis
-- This allows you to test without calling the exchange rate API first

-- CNY to SGD (China to Singapore)
INSERT INTO exchange_rate (from_currency, to_currency, rate, rate_date, created_at, updated_at)
VALUES 
    ('CNY', 'SGD', 0.1850, CURRENT_DATE, NOW(), NOW()),
    ('CNY', 'SGD', 0.1845, CURRENT_DATE - INTERVAL '7 days', NOW(), NOW()),
    ('CNY', 'SGD', 0.1840, CURRENT_DATE - INTERVAL '14 days', NOW(), NOW()),
    ('CNY', 'SGD', 0.1835, CURRENT_DATE - INTERVAL '21 days', NOW(), NOW()),
    ('CNY', 'SGD', 0.1830, CURRENT_DATE - INTERVAL '28 days', NOW(), NOW());

-- CNY to USD (China to US)
INSERT INTO exchange_rate (from_currency, to_currency, rate, rate_date, created_at, updated_at)
VALUES 
    ('CNY', 'USD', 0.1385, CURRENT_DATE, NOW(), NOW()),
    ('CNY', 'USD', 0.1382, CURRENT_DATE - INTERVAL '7 days', NOW(), NOW()),
    ('CNY', 'USD', 0.1378, CURRENT_DATE - INTERVAL '14 days', NOW(), NOW()),
    ('CNY', 'USD', 0.1375, CURRENT_DATE - INTERVAL '21 days', NOW(), NOW()),
    ('CNY', 'USD', 0.1380, CURRENT_DATE - INTERVAL '28 days', NOW(), NOW());

-- GBP to USD (UK to US)
INSERT INTO exchange_rate (from_currency, to_currency, rate, rate_date, created_at, updated_at)
VALUES 
    ('GBP', 'USD', 1.2750, CURRENT_DATE, NOW(), NOW()),
    ('GBP', 'USD', 1.2720, CURRENT_DATE - INTERVAL '7 days', NOW(), NOW()),
    ('GBP', 'USD', 1.2680, CURRENT_DATE - INTERVAL '14 days', NOW(), NOW());

-- Verify
SELECT from_currency, to_currency, COUNT(*) as rate_count
FROM exchange_rate
GROUP BY from_currency, to_currency
ORDER BY from_currency, to_currency;

