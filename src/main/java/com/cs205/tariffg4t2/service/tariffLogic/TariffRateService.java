package com.cs205.tariffg4t2.service.tariffLogic;

import com.cs205.tariffg4t2.service.data.WebScrapingService;
import com.cs205.tariffg4t2.dto.request.TariffCalculationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TariffRateService {

    @Autowired
    private TariffCacheService tariffCacheService;

    @Autowired
    private WebScrapingService webScrapingService;

    public BigDecimal calculateAdValoremRate(TariffCalculationRequest request) {
        BigDecimal rate = tariffCacheService.getCachedAdValoremRate(request);
        if (rate == null) {
            rate = BigDecimal.valueOf(0.3);
            tariffCacheService.cacheAdValoremRate(request, rate);
        }
        return rate;
    }

    public BigDecimal calculateSpecificRate(TariffCalculationRequest request) {
        BigDecimal ratePerUnit = tariffCacheService.getCachedSpecificRate(request);
        if (ratePerUnit == null) {
            ratePerUnit = BigDecimal.valueOf(0.3);
            tariffCacheService.cacheSpecificRate(request, ratePerUnit);
        }
        return ratePerUnit.multiply(request.getQuantity());
    }
}
