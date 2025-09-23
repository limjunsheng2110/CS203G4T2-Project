package com.cs205.tariffg4t2.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class TariffCalculationRequest {
    private String homeCountry;
    private String destinationCountry;
    private String productCategory;
    private BigDecimal productValue;
    private String hsCode;
    private String tradeAgreement;
}