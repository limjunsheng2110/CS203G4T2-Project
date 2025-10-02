package com.CS203.tariffg4t2.dto.response;

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
public class TariffCalculationResultDTO {
    //difference between TariffCalculationResult and TariffCalculationResponse is
    //additional fields tarrifRate, tariffAmount, totalCost, currency, tradeAgreement, calculationDate

    private String importingCountry;
    private String exportingCountry;
    private String hsCode;
    private String productDescription;
    private BigDecimal productValue;
    private BigDecimal quantity;
    private Integer heads;
    private BigDecimal tariffAmount;
    private BigDecimal shippingCost;
    private BigDecimal totalCost;
    private String TariffType;
    private LocalDateTime calculationDate;
    private String tradeAgreement;
    private BigDecimal shippingRate;
    private BigDecimal adValoremRate;
    private BigDecimal specificRate;
    private BigDecimal adValoremPreferentialRate;
    private BigDecimal specificPreferentialRate;
}