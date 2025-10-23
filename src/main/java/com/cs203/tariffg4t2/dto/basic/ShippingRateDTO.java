package com.cs203.tariffg4t2.dto.basic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingRateDTO {
    private String importingCountryCode;
    private String exportingCountryCode;
    private BigDecimal airRate;
    private BigDecimal seaRate;
    private BigDecimal landRate;
    private BigDecimal distance;
}

