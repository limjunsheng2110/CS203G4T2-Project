package com.cs205.tariffg4t2.service.tariffLogic;

import com.cs205.tariffg4t2.dto.request.TariffCalculationRequest;
import com.cs205.tariffg4t2.dto.response.TariffCalculationResult;
import com.cs205.tariffg4t2.service.data.WebScrapingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class TariffCalculatorService {

    @Autowired
    private WebScrapingService webScrapingService;

    @Autowired
    private TariffFTAService tariffFTAService;

    @Autowired
    private TariffCacheService tariffCacheService;

    @Autowired
    private TariffValidationService tariffValidationService;

    public TariffCalculationResult calculateTariff(TariffCalculationRequest request) {
        
        // Step 0: Validate input
        List<String> validationErrors = tariffValidationService.validateTariffRequest(request);
        if (!validationErrors.isEmpty()) {
            throw new IllegalArgumentException("Validation errors: " + String.join(", ", validationErrors));
        }

        // Step 1: Get the tariff rate (from cache or scraping)
        BigDecimal tariffRate = getTariffRate(request);
        
        // Step 2: Apply trade agreement adjustments
        BigDecimal adjustedRate = tariffFTAService.applyTradeAgreementDiscount(
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
        String tradeAgreement = tariffFTAService.getApplicableTradeAgreement(
            request.getHomeCountry(),
            request.getDestinationCountry()
        );
        
        return TariffCalculationResult.builder()
                .homeCountry(request.getHomeCountry())
                .destinationCountry(request.getDestinationCountry())
                .productName(request.getProductName())
                .productValue(request.getProductValue())
                .tariffRate(adjustedRate)
                .tariffAmount(tariffAmount)
                .totalCost(totalCost)
                .currency("USD")
                .tradeAgreement(tradeAgreement)
                .calculationDate(LocalDateTime.now())
                .build();
    }


    //This checks whether or not the tariff rate is in cache. If not, then it will just call the web scraping service to get the rate.
    private BigDecimal getTariffRate(TariffCalculationRequest request) {
        // Check cache first
        BigDecimal cachedRate = tariffCacheService.getCachedRate(request);
        if (cachedRate != null) {
            return cachedRate;
        }
        
        // Cache miss or expired - get fresh data
        BigDecimal rate = fetchTariffRate(request);
        
        // Cache the result
        tariffCacheService.cacheRate(request, rate);

        return rate;
    }

    private BigDecimal fetchTariffRate(TariffCalculationRequest request) {
        try {
            // Delegate actual scraping to the dedicated service
            return webScrapingService.getTariffRate(
                request.getHomeCountry(),
                request.getDestinationCountry(),
                request.getHsCode(),
                request.getProductName()
            );

        } catch (Exception e) {
            // Log the error and return fallback rate
            System.err.println("Failed to scrape tariff rate: " + e.getMessage());
            return getFallbackTariffRate(request.getProductName());
        }
    }

    private BigDecimal getFallbackTariffRate(String productName) {
        // Fallback rates based on common product categories
        Map<String, BigDecimal> fallbackRates = Map.of(
            "Beef", new BigDecimal("15.0"),
            "Chicken", new BigDecimal("5.0")
        );
        
        return fallbackRates.getOrDefault(productName, new BigDecimal("10.0"));
    }
}