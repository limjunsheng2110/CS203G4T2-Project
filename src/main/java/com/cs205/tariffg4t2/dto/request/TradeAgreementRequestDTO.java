package com.cs205.tariffg4t2.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeAgreementRequestDTO {

    @NotBlank(message = "Trade agreement name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Trade agreement type is required")
    @Size(max = 100, message = "Type must not exceed 100 characters")
    private String type;

    private Set<String> memberCountryCodes;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;

    private LocalDate expiryDate; // Optional
}
