package com.cs205.tariffg4t2.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TariffCalculationResult {
    //difference between TariffCalculationResult and TariffCalculationResponse is
    //additional fields tarrifRate, tariffAmount, totalCost, currency, tradeAgreement, calculationDate

    public enum TariffType { AD_VALOREM, SPECIFIC }

    private String homeCountry;
    private String destinationCountry;
    private String productName;
    private BigDecimal productValue;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal tariffAmount;
    private BigDecimal shippingCost;
    private LocalDateTime calculationDate;
}