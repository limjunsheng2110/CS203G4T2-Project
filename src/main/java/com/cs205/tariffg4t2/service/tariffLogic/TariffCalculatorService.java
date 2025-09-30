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

    @Autowired
    TariffValidationService tariffValidationService;

    public TariffCalculationResult calculateTariff(TariffCalculationRequest request) {
        // First validate the request using TariffValidationService
        tariffValidationService.validateTariffRequest(request);

        //then call TariffRateService to calculate based on ad valorem or specific
        //it should return an amount.

        BigDecimal dutyAmount;
        dutyAmount = tariffRateService.calculateTariffAmount(request);

        // Apply FTA discount, so preferential rates. Call TariffFTAService
        dutyAmount = tariffFTAService.applyTradeAgreementDiscount(request, dutyAmount);

        // Calculate shipping cost using ShippingCostService, return amount
        BigDecimal shippingCost = shippingCostService.calculateShippingCost(request);


        //Total Cost = product value + duty amount + shipping cost
        BigDecimal totalCost = request.getProductValue().add(dutyAmount).add(shippingCost);

        // Build and return result

        return TariffCalculationResult.builder()
                .homeCountry(request.getHomeCountry())
                .destinationCountry(request.getDestinationCountry())
                .productName(request.getProductName())
                .productValue(request.getProductValue())
                .quantity(request.getQuantity())
                .unit(request.getUnit())
                .tariffAmount(dutyAmount.setScale(2, RoundingMode.HALF_UP))
                .shippingCost(shippingCost.setScale(2, RoundingMode.HALF_UP))
                .totalCost(totalCost.setScale(2, RoundingMode.HALF_UP))
                .calculationDate(LocalDateTime.now())
                .build();
    }

}

