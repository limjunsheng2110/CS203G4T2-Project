package com.cs205.tariffg4t2.dto;

import com.cs205.tariffg4t2.model.basic.TariffRate;
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
    private String unit;
    private TariffRate.TariffType tariffType = TariffRate.TariffType.AD_VALOREM;
    private BigDecimal adValoremRate;
    private BigDecimal specificRateAmount;
}

