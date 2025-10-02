package com.cs205.tariffg4t2.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeAgreementDTO {

    @NotBlank(message = "Name cannot be blank")
    private String name;

    @NotNull(message = "Effective date cannot be null")
    private LocalDate effectiveDate;

    @NotNull(message = "Expiry date cannot be null")
    private LocalDate expiryDate;
}

