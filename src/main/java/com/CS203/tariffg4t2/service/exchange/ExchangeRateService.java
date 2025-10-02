package com.CS203.tariffg4t2.service.exchange;

import com.CS203.tariffg4t2.model.exchange.ExchangeRate;
import com.CS203.tariffg4t2.repository.exchange.ExchangeRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ExchangeRateService {
    
    @Autowired
    private ExchangeRateRepository exchangeRateRepository;
    
    @Value("${exchangerate.api.key:YOUR_API_KEY_HERE}")
    private String apiKey;
    
    @Value("${exchangerate.api.url:https://api.exchangerate-api.com/v4/latest/}")
    private String apiUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final int CACHE_HOURS = 24;
    
    /**
     * Get exchange rate with caching - checks DB first, then API if needed
     */
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        // If same currency, return 1
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return BigDecimal.ONE;
        }
        
        // Check cache first
        Optional<ExchangeRate> cachedRate = exchangeRateRepository.findRecentRate(
            fromCurrency.toUpperCase(),
            toCurrency.toUpperCase(),
            LocalDateTime.now().minusHours(CACHE_HOURS)
        );
        
        if (cachedRate.isPresent()) {
            return cachedRate.get().getRate();
        }
        
        // Fetch from API if not in cache
        return fetchAndCacheExchangeRate(fromCurrency, toCurrency);
    }
    
    /**
     * Convert amount from one currency to another
     */
    public BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Fetch exchange rate from external API and cache it
     */
    private BigDecimal fetchAndCacheExchangeRate(String fromCurrency, String toCurrency) {
        try {
            String url = apiUrl + fromCurrency.toUpperCase();
            String response = restTemplate.getForObject(url, String.class);
            
            JsonNode root = objectMapper.readTree(response);
            JsonNode rates = root.get("rates");
            
            if (rates == null || !rates.has(toCurrency.toUpperCase())) {
                throw new RuntimeException("Currency not found: " + toCurrency);
            }
            
            BigDecimal rate = rates.get(toCurrency.toUpperCase()).decimalValue();
            
            // Cache the rate
            ExchangeRate exchangeRate = new ExchangeRate();
            exchangeRate.setFromCurrency(fromCurrency.toUpperCase());
            exchangeRate.setToCurrency(toCurrency.toUpperCase());
            exchangeRate.setRate(rate);
            exchangeRate.setTimestamp(LocalDateTime.now());
            exchangeRate.setSource("exchangerate-api.com");
            
            exchangeRateRepository.save(exchangeRate);
            
            return rate;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch exchange rate: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if a currency is stronger/cheaper compared to another
     * Returns true if fromCurrency is stronger (you get more toCurrency per unit)
     */
    public boolean isCurrencyStronger(String currency1, String currency2, String baseCurrency) {
        BigDecimal rate1 = getExchangeRate(baseCurrency, currency1);
        BigDecimal rate2 = getExchangeRate(baseCurrency, currency2);
        return rate1.compareTo(rate2) > 0;
    }
    
    /**
     * Clean up old exchange rates (run as scheduled task)
     */
    public void cleanupOldRates() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        exchangeRateRepository.deleteByTimestampBefore(cutoff);
    }
}