package com.cs205.tariffg4t2.service.tariffLogic;

import com.cs205.tariffg4t2.dto.request.TariffCalculationRequestDTO;
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

    public TariffCalculationResult calculateTariff(TariffCalculationRequestDTO request) {

        System.out.println("Received tariff calculation request: " + request);

        // First validate the request using TariffValidationService
        tariffValidationService.validateTariffRequest(request);

        System.out.println("Request validated successfully.");

        //then call TariffRateService to calculate based on ad valorem or specific
        //it should return an amount.

        BigDecimal dutyAmount;
        dutyAmount = tariffRateService.calculateTariffAmount(request);

        System.out.println("Initial calculated duty amount: " + dutyAmount);

        // get preferential rate from TariffFTAService
        BigDecimal preferentialRate = tariffFTAService.applyTradeAgreementDiscount(request, dutyAmount);

        if (preferentialRate != null) {
            dutyAmount = request.getProductValue().multiply(preferentialRate).setScale(2, RoundingMode.HALF_UP);
        }


        System.out.println("Discounted duty amount after FTA: " + dutyAmount);

        // Calculate shipping cost using ShippingCostService, return amount
        BigDecimal shippingCost = shippingCostService.calculateShippingCost(request);

        System.out.println("Calculated shipping cost: " + shippingCost);
        System.out.println("Calculated duty amount: " + dutyAmount);
        System.out.println("Product value: " + request.getProductValue());

        //Total Cost = product value + duty amount + shipping cost
        BigDecimal totalCost = request.getProductValue().add(dutyAmount).add(shippingCost);

        // Build and return result

        return TariffCalculationResult.builder()
                .importingCountry(request.getImportingCountry())
                .exportingCountry(request.getExportingCountry())
                .productValue(request.getProductValue())
                .hsCode(request.getHsCode())
                .quantity(request.getQuantity())
                .unit(request.getUnit())
                .tariffAmount(dutyAmount.setScale(2, RoundingMode.HALF_UP))
                .shippingCost(shippingCost.setScale(2, RoundingMode.HALF_UP))
                .totalCost(totalCost.setScale(2, RoundingMode.HALF_UP))
                .calculationDate(LocalDateTime.now())
                .build();
    }

}

