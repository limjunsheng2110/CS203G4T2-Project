package com.cs205.tariffg4t2.service.tariffLogic;

import com.cs205.tariffg4t2.dto.request.TariffCalculationRequest;
import com.cs205.tariffg4t2.dto.response.TariffCalculationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class TariffCalculatorService {

    @Autowired
    private TariffRateService tariffRateService;

    @Autowired
    private TariffFTAService tariffFTAService;

    @Autowired
    private ShippingCostService shippingCostService;

    public TariffCalculationResult calculateTariff(TariffCalculationRequest request) {
        // Validate input
        validateRequest(request);

        // Determine tariff type (Ad Valorem or Specific)
        boolean hasProductValue = request.getProductValue() != null;
        boolean hasQuantityAndUnit = request.getQuantity() != null && request.getUnit() != null;

        if (!hasProductValue && !hasQuantityAndUnit) {
            throw new IllegalArgumentException("Provide either productValue or quantity and unit.");
        }

        // Initialize result variables
        BigDecimal dutyAmount = BigDecimal.ZERO;
        BigDecimal additionalCost = BigDecimal.ZERO;
        String tariffType = null;

        // Determine calculation type: Ad Valorem or Specific
        if (hasProductValue) {
            // Ad Valorem calculation
            dutyAmount = tariffRateService.calculateAdValoremRate(request);
            tariffType = "AD_VALOREM";
        } else if (hasQuantityAndUnit) {
            // Specific calculation
            dutyAmount = tariffRateService.calculateSpecificRate(request);
            additionalCost = shippingCostService.calculateShippingCost(request);
            tariffType = "SPECIFIC";
        }

        // Apply FTA discount (if applicable)
        BigDecimal finalRate = tariffFTAService.applyTradeAgreementDiscount(dutyAmount, request.getHomeCountry(), request.getDestinationCountry());

        // Build and return result
        return TariffCalculationResult.builder()
                .homeCountry(request.getHomeCountry())
                .destinationCountry(request.getDestinationCountry())
                .productName(request.getProductName())
                .productValue(request.getProductValue())
                .quantity(request.getQuantity())
                .unit(request.getUnit())
                .tariffAmount(finalRate)
                .shippingCost(additionalCost)
                .calculationDate(LocalDateTime.now())
                .build();
    }

    private void validateRequest(TariffCalculationRequest request) {
        // Simple validation for missing fields, assuming they must have either productValue or quantity/unit
        if (request.getProductValue() == null && (request.getQuantity() == null || request.getUnit() == null)) {
            throw new IllegalArgumentException("Either productValue or quantity and unit must be provided.");
        }
    }
}

