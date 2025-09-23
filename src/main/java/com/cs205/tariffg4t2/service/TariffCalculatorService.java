package com.cs205.tariffg4t2.service;

import com.cs205.tariffg4t2.dto.request.TariffCalculationRequest;
import com.cs205.tariffg4t2.dto.response.TariffCalculationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class TariffCalculatorService {

    @Autowired
    private WebScrapingService webScrapingService;

    // Simple in-memory cache - replace with Redis in production
    private final Map<String, CachedTariff> tariffCache = new HashMap<>();
    private static final long CACHE_DURATION_MS = 24 * 60 * 60 * 1000; // 24 hours

    
    public TariffCalculationResult calculateTariff(TariffCalculationRequest request) {
        
        // Step 1: Get the tariff rate (from cache or scraping)
        BigDecimal tariffRate = getTariffRate(request);
        
        // Step 2: Apply trade agreement adjustments
        BigDecimal adjustedRate = applyTradeAgreementDiscount(
            tariffRate, 
            request.getHomeCountry(), 
            request.getDestinationCountry()
        );
        
        // Step 3: Calculate amounts
        BigDecimal tariffAmount = request.getProductValue()
                .multiply(adjustedRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        BigDecimal totalCost = request.getProductValue().add(tariffAmount);
        
        // Step 4: Determine which trade agreement was applied
        String tradeAgreement = getApplicableTradeAgreement(
            request.getHomeCountry(), 
            request.getDestinationCountry()
        );
        
        return TariffCalculationResult.builder()
                .homeCountry(request.getHomeCountry())
                .destinationCountry(request.getDestinationCountry())
                .productCategory(request.getProductCategory())
                .productValue(request.getProductValue())
                .tariffRate(adjustedRate)
                .tariffAmount(tariffAmount)
                .totalCost(totalCost)
                .currency("USD")
                .tradeAgreement(tradeAgreement)
                .calculationDate(LocalDateTime.now())
                .build();
    }

    private BigDecimal getTariffRate(TariffCalculationRequest request) {
        String cacheKey = buildCacheKey(request);
        
        // Check cache first
        CachedTariff cached = tariffCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.getRate();
        }
        
        // Cache miss or expired - get fresh data
        BigDecimal rate = fetchTariffRate(request);
        
        // Cache the result
        tariffCache.put(cacheKey, new CachedTariff(rate, System.currentTimeMillis()));
        
        return rate;
    }

    private BigDecimal fetchTariffRate(TariffCalculationRequest request) {
        try {
            // Delegate actual scraping to the dedicated service
            return webScrapingService.getTariffRate(
                request.getHomeCountry(),
                request.getDestinationCountry(),
                request.getHsCode(),
                request.getProductCategory()
            );
            
        } catch (Exception e) {
            // Log the error and return fallback rate
            System.err.println("Failed to scrape tariff rate: " + e.getMessage());
            return getFallbackTariffRate(request.getProductCategory());
        }
    }

    private BigDecimal applyTradeAgreementDiscount(BigDecimal baseRate, String homeCountry, String destinationCountry) {
        // Apply trade agreement discounts
        String agreement = getApplicableTradeAgreement(homeCountry, destinationCountry);
        
        switch (agreement) {
            case "USMCA":
                return baseRate.multiply(BigDecimal.valueOf(0.5)); // 50% discount
            case "EU Trade Agreement":
                return baseRate.multiply(BigDecimal.valueOf(0.7)); // 30% discount
            case "WTO Most Favored Nation":
                return baseRate.multiply(BigDecimal.valueOf(0.9)); // 10% discount
            default:
                return baseRate; // No discount
        }
    }

    private String getApplicableTradeAgreement(String homeCountry, String destinationCountry) {
        // Simple trade agreement logic - expand based on real agreements
        if (isNAFTACountry(homeCountry) && isNAFTACountry(destinationCountry)) {
            return "USMCA";
        } else if (isEUCountry(homeCountry) && isEUCountry(destinationCountry)) {
            return "EU Single Market";
        } else if (isWTOMember(homeCountry) && isWTOMember(destinationCountry)) {
            return "WTO Most Favored Nation";
        }
        return "Standard Rate";
    }

    private BigDecimal getFallbackTariffRate(String productCategory) {
        // Fallback rates based on common product categories
        Map<String, BigDecimal> fallbackRates = Map.of(
            "Electronics", new BigDecimal("15.0"),
            "Textiles", new BigDecimal("12.5"),
            "Automotive", new BigDecimal("8.0"),
            "Agriculture", new BigDecimal("20.0"),
            "Machinery", new BigDecimal("10.0")
        );
        
        return fallbackRates.getOrDefault(productCategory, new BigDecimal("10.0"));
    }

    private String buildCacheKey(TariffCalculationRequest request) {
        return String.format("%s_%s_%s_%s", 
            request.getHomeCountry(),
            request.getDestinationCountry(),
            request.getHsCode() != null ? request.getHsCode() : "GENERAL",
            request.getProductCategory()
        );
    }

    private boolean isNAFTACountry(String country) {
        return "USA".equalsIgnoreCase(country) || 
               "CAN".equalsIgnoreCase(country) || 
               "MEX".equalsIgnoreCase(country);
    }

    private boolean isEUCountry(String country) {
        // Simplified - add more EU country codes
        return "DEU".equalsIgnoreCase(country) || 
               "FRA".equalsIgnoreCase(country) || 
               "ITA".equalsIgnoreCase(country);
    }

    private boolean isWTOMember(String country) {
        // Most countries are WTO members - simplified check
        return true;
    }

    // Inner class for caching
    private static class CachedTariff {
        private final BigDecimal rate;
        private final long timestamp;

        public CachedTariff(BigDecimal rate, long timestamp) {
            this.rate = rate;
            this.timestamp = timestamp;
        }

        public BigDecimal getRate() {
            return rate;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
        }
    }
}