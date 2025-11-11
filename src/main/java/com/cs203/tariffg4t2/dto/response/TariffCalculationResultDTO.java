package com.cs203.tariffg4t2.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TariffCalculationResultDTO {
    // Inputs echoed
    private String importingCountry;
    private String exportingCountry;
    private String hsCode;
    private BigDecimal productValue;
    private BigDecimal totalWeight;
    private Integer heads;
    private String TariffType;

    // Breakdown
    private BigDecimal customsValue;
    private BigDecimal baseDuty;
    private BigDecimal vatOrGst;
    private BigDecimal shippingCost;

    // Totals
    private BigDecimal tariffAmount;       // baseDuty
    private BigDecimal totalCost;          // customsValue + tariffAmount + vat/gst + shipping

    // Meta
    private String tradeAgreement;
    private java.time.LocalDateTime calculationDate;
    private Integer year;  // Year from tariff data

    // Rates shown for transparency
    private BigDecimal adValoremRate;
    private BigDecimal vatRate;  // VAT/GST rate as percentage
    private BigDecimal shippingRatePerKg;  // Shipping rate per kilogram
}
