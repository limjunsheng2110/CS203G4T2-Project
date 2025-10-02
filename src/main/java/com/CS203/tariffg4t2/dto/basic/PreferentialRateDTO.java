package com.CS203.tariffg4t2.dto.basic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferentialRateDTO {

    @NotNull(message = "Trade agreement ID cannot be null")
    private Long tradeAgreementId;

    @NotBlank(message = "HS code cannot be blank")
    private String hsCode;

    @NotNull(message = "Origin country ID cannot be null")
    private String originCountryId;

    @NotNull(message = "Destination country ID cannot be null")
    private String destinationCountryId;

    @NotNull(message = "Preferential rate cannot be null")
    @PositiveOrZero(message = "Preferential rate must be zero or positive")
    private BigDecimal preferentialRate;
}

