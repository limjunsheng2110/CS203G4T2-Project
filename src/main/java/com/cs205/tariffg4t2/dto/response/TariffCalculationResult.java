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
    private String homeCountry;
    private String destinationCountry;
    private String productName;
    private BigDecimal productValue;
    private BigDecimal tariffRate;
    private BigDecimal tariffAmount;
    private BigDecimal totalCost;
    private String currency;
    private String tradeAgreement;
    private LocalDateTime calculationDate;
}