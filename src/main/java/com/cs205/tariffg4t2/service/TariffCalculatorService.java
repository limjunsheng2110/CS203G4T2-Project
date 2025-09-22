package com.cs205.tariffg4t2.service;

import com.cs205.tariffg4t2.dto.request.TariffCalculationRequest;
import com.cs205.tariffg4t2.dto.response.TariffCalculationResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
public class TariffCalculatorService {

    public TariffCalculationResult calculateTariff(TariffCalculationRequest request) {

        // Mock tariff rate for testing - 10% tariff
        BigDecimal mockTariffRate = new BigDecimal("10.00");

        // Calculate tariff amount
        BigDecimal tariffAmount = request.getProductValue()
                .multiply(mockTariffRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Calculate total cost
        BigDecimal totalCost = request.getProductValue().add(tariffAmount);


        return TariffCalculationResult.builder()
                .homeCountry(request.getHomeCountry())
                .destinationCountry(request.getDestinationCountry())
                .productCategory(request.getProductCategory())
                .productValue(request.getProductValue())
                .tariffRate(mockTariffRate)
                .tariffAmount(tariffAmount)
                .totalCost(totalCost)
                .currency("USD")
                .tradeAgreement("Standard Rate")
                .calculationDate(LocalDateTime.now())
                .build();
    }

}