package com.CS203.tariffg4t2.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// import basic.model.com.CS203.tariffg4t2.ShippingRate.ShippingMode;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class TariffCalculationRequestDTO {

    // public enum ShippingMode { SEA, AIR, LAND }

    private String importingCountry;
    private String exportingCountry;
    private BigDecimal productValue;
    private String hsCode;
    private Integer heads;
    private BigDecimal weight;
    private String shippingMode;
}