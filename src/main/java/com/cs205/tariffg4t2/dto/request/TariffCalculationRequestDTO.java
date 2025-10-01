package com.cs205.tariffg4t2.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// import com.cs205.tariffg4t2.model.basic.ShippingRate.ShippingMode;


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
    private BigDecimal quantity;
    // Unit used by SPECIFIC tariff (e.g., "kg", "pieces").
    private String unit;
    private String shippingMode;
}