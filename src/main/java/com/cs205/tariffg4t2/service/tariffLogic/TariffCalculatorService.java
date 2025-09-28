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
        
        // Validate input
        List<String> validationErrors = tariffValidationService.validateTariffRequest(request);
        if (!validationErrors.isEmpty()) {
            throw new IllegalArgumentException("Validation errors: " + String.join(", ", validationErrors));
        }

        boolean hasValue = request.getProductValue() != null;
        boolean hasQtyAndUnit = request.getQuantity() != null
                && request.getUnit() != null
                && !request.getUnit().isBlank();
        if (!hasValue && !hasQtyAndUnit) {
            throw new IllegalArgumentException("Provide productValue or (quantity and unit).");
        }

    // For display only
    String tradeAgreement = tariffFTAService.getApplicableTradeAgreement(
            request.getHomeCountry(), request.getDestinationCountry()
    );

    // ===== Try ad valorem if possible =====
    BigDecimal adValoremRatePct = null;
    BigDecimal adValoremDuty = null;
    if (hasValue) {
        BigDecimal rawPct = getTariffRate(request); // e.g., 5.0 means 5%
        BigDecimal adjustedPct = tariffFTAService.applyTradeAgreementDiscount(
                rawPct, 
                request.getHomeCountry(), 
                request.getDestinationCountry()
        );
        adValoremRatePct = adjustedPct;
        adValoremDuty = request.getProductValue()
                .multiply(adValoremRatePct)
                .divide(BigDecimal.valueOf(100), 
                        2, 
                        RoundingMode.HALF_UP);
    }

    // ===== Try specific if possible =====
    BigDecimal specificPerUnit = null;
    BigDecimal specificDuty = null;
    BigDecimal shipping = BigDecimal.ZERO;
    if (hasQtyAndUnit) {
        BigDecimal rawPerUnit = getSpecificRatePerUnit(request); // currency per unit
        BigDecimal adjustedPerUnit = tariffFTAService.applyTradeAgreementDiscountOnSpecific(
                rawPerUnit, 
                request.getHomeCountry(), 
                request.getDestinationCountry()
        );
        specificPerUnit = adjustedPerUnit;
        specificDuty = specificPerUnit
                .multiply(request.getQuantity())
                .setScale(2, RoundingMode.HALF_UP);

        // v1: shipping only used with specific path
        if (request.getShippingAmount() != null) {
            shipping = request.getShippingAmount().setScale(2, RoundingMode.HALF_UP);
        }
    }

    // ===== Choose the path =====
    String chosenType;
    BigDecimal chosenDuty;
    BigDecimal chosenAdditionalCost;
    BigDecimal chosenAdValoremRate = null;
    BigDecimal chosenSpecificPerUnit = null;
    BigDecimal chosenShipping = BigDecimal.ZERO;

    if (adValoremDuty != null) {
        chosenType = "AD_VALOREM";
        chosenDuty = adValoremDuty;
        chosenAdValoremRate = adValoremRatePct;
        chosenShipping = BigDecimal.ZERO;
        chosenAdditionalCost = chosenDuty; // duty only in v1
    } else if (specificDuty != null) {
        chosenType = "SPECIFIC";
        chosenDuty = specificDuty;
        chosenSpecificPerUnit = specificPerUnit;
        chosenShipping = shipping;
        chosenAdditionalCost = chosenDuty.add(chosenShipping);
    } else {
        // AUTO (default to highest)
        boolean autoLowest = "AUTO_LOWEST".equalsIgnoreCase(mode);

        // Compare duties (note: only specific has shipping in v1)
        BigDecimal adValoremTotal = adValoremDuty;              // no shipping
        BigDecimal specificTotal = specificDuty.add(shipping);  // includes shipping

        boolean chooseSpecific = autoLowest ? specificTotal.compareTo(adValoremTotal) <= 0
                                            : specificTotal.compareTo(adValoremTotal) >= 0;

        if (chooseSpecific) {
            chosenType = "SPECIFIC";
            chosenDuty = specificDuty;
            chosenSpecificPerUnit = specificPerUnit;
            chosenShipping = shipping;
            chosenAdditionalCost = specificTotal;
        } else {
            chosenType = "AD_VALOREM";
            chosenDuty = adValoremDuty;
            chosenAdValoremRate = adValoremRatePct;
            chosenShipping = BigDecimal.ZERO;
            chosenAdditionalCost = adValoremDuty;
        }
    }

    // ===== Build response =====
    return TariffCalculationResult.builder()
            .homeCountry(request.getHomeCountry())
            .destinationCountry(request.getDestinationCountry())
            .productName(request.getProductName())
            .productValue(request.getProductValue()) // may be null on SPECIFIC path
            .adValoremRate(chosenAdValoremRate)      // non-null only if AD_VALOREM chosen
            .specificRatePerUnit(chosenSpecificPerUnit) // non-null only if SPECIFIC chosen (if your DTO has it)
            .tariffAmount(chosenDuty)
            .shipping(chosenShipping)
            .additionalCost(chosenAdditionalCost)    // duty (+ shipping if specific)
            .tariffType(chosenType)
            .currency("USD")
            .tradeAgreement(tradeAgreement)
            .calculationDate(LocalDateTime.now())
            .build();


    //This checks whether or not the tariff rate is in cache. If not, then it will just call the web scraping service to get the rate.
    private BigDecimal getTariffRate(TariffCalculationRequest request) {
        // Check cache first
        BigDecimal cachedRate = tariffCacheService.getCachedAdValoremRate(request);
        if (cachedRate != null) {
            return cachedRate;
        }
        
        // Cache miss or expired - get fresh data
        BigDecimal rate = fetchTariffRate(request);
        
        // Cache the result
        tariffCacheService.cacheAdValoremRate(request, rate);

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

    private BigDecimal getSpecificRatePerUnit(TariffCalculationRequest request) {
        // 1) try cache
        BigDecimal cached = tariffCacheService.getCachedSpecificRate(request);
        if (cached != null) return cached;
    
        // 2) fetch (scrape/DB/API)
        BigDecimal rate = webScrapingService.getSpecificRatePerUnit(
                request.getHomeCountry(),
                request.getDestinationCountry(),
                request.getHsCode(),
                request.getProductName(),
                request.getUnit() // currency per this unit
        );
    
        // 3) fallback if source not ready yet
        if (rate == null) {
            rate = getSpecificFallbackRate(request.getProductName(), request.getUnit());
        }
    
        // 4) cache it
        tariffCacheService.cacheSpecificRate(request, rate);
        return rate;
    }
    
    private BigDecimal getSpecificFallbackRate(String productName, String unit) {
        // Example only: currency per kg (tune as you like)
        if (unit != null && unit.equalsIgnoreCase("kg")) {
            Map<String, BigDecimal> perKg = Map.of(
                "Beef", new BigDecimal("0.80"),
                "Chicken", new BigDecimal("0.30")
            );
            return perKg.getOrDefault(productName, new BigDecimal("0.50"));
        }
        // Default fallback
        return new BigDecimal("1.00");
    }
    
}