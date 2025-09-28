package com.cs205.tariffg4t2.service.tariffLogic;

import com.cs205.tariffg4t2.dto.request.TariffCalculationRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class TariffCacheService {

    // Simple in-memory cache - replace with Redis in production
    private final Map<String, CachedTariff> tariffCache = new HashMap<>();
    private static final long CACHE_DURATION_MS = 24 * 60 * 60 * 1000; // 24 hours

    public BigDecimal getCachedAdValoremRate(TariffCalculationRequest request) {
        String cacheKey = buildCacheAdValoremKey(request);
        CachedTariff cached = tariffCache.get(cacheKey);

        if (cached != null && !cached.isExpired()) {
            return cached.getRate();
        }

        return null; // Cache miss or expired
    }

    public void cacheAdValoremRate(TariffCalculationRequest request, BigDecimal rate) {
        String cacheKey = buildCacheAdValoremKey(request);
        tariffCache.put(cacheKey, new CachedTariff(rate, System.currentTimeMillis()));
    }

    private String buildCacheAdValoremKey(TariffCalculationRequest request) {
        return String.format("%s_%s_%s_%s",
            request.getHomeCountry(),
            request.getDestinationCountry(),
            request.getHsCode() != null ? request.getHsCode() : "GENERAL",
            request.getProductName()
        );
    }

    
    public BigDecimal getCachedSpecificRate(TariffCalculationRequest request) {
        String cacheKey = buildCacheSpecificKey(request);
        CachedTariff cached = tariffCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.getRate();
        }
        return null; // Cache miss or expired
    }

    public void cacheSpecificRate(TariffCalculationRequest request, BigDecimal rate) {
        String cacheKey = buildCacheSpecificKey(request);
        tariffCache.put(cacheKey, new CachedTariff(rate, System.currentTimeMillis()));
    }

    private String buildCacheSpecificKey(TariffCalculationRequest request) {
        // Prefix to avoid any overlap with ad valorem keys, and include unit since rate is currency-per-unit
        return String.format("SPEC_%s_%s_%s_%s_%s",
            request.getHomeCountry(),
            request.getDestinationCountry(),
            request.getHsCode() != null ? request.getHsCode() : "GENERAL",
            request.getProductName(),
            request.getUnit() != null ? request.getUnit() : "UNITLESS"
        );
    }

    public void clearCache() {
        tariffCache.clear();
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

