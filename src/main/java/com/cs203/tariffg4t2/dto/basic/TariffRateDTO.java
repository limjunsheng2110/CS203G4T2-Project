package com.cs203.tariffg4t2.dto.basic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TariffRateDTO {

    private String hsCode;
    private String importingCountryCode;
    private String exportingCountryCode;
    private BigDecimal baseRate;
    private String tariffType;
    private String date;
}
