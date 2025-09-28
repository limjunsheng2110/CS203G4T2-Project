package com.cs205.tariffg4t2.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import com.cs205.tariffg4t2.model.basic.ShippingRate.ShippingMode;
import com.cs205.tariffg4t2.model.basic.ShippingRate.ShippingRateType;

import jakarta.validation.constraints.DecimalMin;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class TariffCalculationRequest {

    public enum ShippingMode { SEA, AIR, LAND }
    public enum ShippingRateType { FLAT, PER_WEIGHT }

    private String homeCountry;
    private String destinationCountry;
    private String productName;
    private BigDecimal productValue;
    private String hsCode;
    private String tradeAgreement;
    private BigDecimal quantity;
    // Unit used by SPECIFIC tariff (e.g., "kg", "pieces").
    private String unit;

    private ShippingMode shippingMode;               // optional metadata
    private ShippingRateType shippingRateType;       // FLAT or PER_WEIGHT
    @DecimalMin(value = "0.0", inclusive = true, message = "shippingRate must be >= 0")
    private BigDecimal shippingRate;                 // If FLAT -> total; If PER_WEIGHT -> per unit
}