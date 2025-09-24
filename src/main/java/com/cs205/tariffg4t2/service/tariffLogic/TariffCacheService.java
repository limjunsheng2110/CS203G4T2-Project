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

    public BigDecimal getCachedRate(TariffCalculationRequest request) {
        String cacheKey = buildCacheKey(request);
        CachedTariff cached = tariffCache.get(cacheKey);

        if (cached != null && !cached.isExpired()) {
            return cached.getRate();
        }

        return null; // Cache miss or expired
    }

    public void cacheRate(TariffCalculationRequest request, BigDecimal rate) {
        String cacheKey = buildCacheKey(request);
        tariffCache.put(cacheKey, new CachedTariff(rate, System.currentTimeMillis()));
    }

    private String buildCacheKey(TariffCalculationRequest request) {
        return String.format("%s_%s_%s_%s",
            request.getHomeCountry(),
            request.getDestinationCountry(),
            request.getHsCode() != null ? request.getHsCode() : "GENERAL",
            request.getProductCategory()
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

