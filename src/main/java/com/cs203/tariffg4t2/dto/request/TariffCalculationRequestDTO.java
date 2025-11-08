package com.cs203.tariffg4t2.dto.request;

import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.constraints.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TariffCalculationRequestDTO {

    // REQUIRED (user)
    @NotBlank private String importingCountry;
    @NotBlank private String exportingCountry;
    @NotBlank private String hsCode;

    @NotNull private BigDecimal productValue;

    @NotNull private Integer heads;      // number of units/items
    @NotNull private BigDecimal weight;   // in kg

    // OPTIONAL (user)
    private String shippingMode;               // "SEA" | "AIR" | "LAND"
    private BigDecimal freight;                // may be null
    private BigDecimal insurance;              // may be null

    // ---- Tester overrides only (optional)
    private String valuationOverride;          // "CIF"/"TRANSACTION" for testing
    private BigDecimal vatOrGstOverride;       // for testing - overrides Country VAT rate

    // Track missing or defaulted fields
    @Builder.Default
    private List<String> missingFields = new ArrayList<>();

    @Builder.Default
    private List<String> defaultedFields = new ArrayList<>();
}
