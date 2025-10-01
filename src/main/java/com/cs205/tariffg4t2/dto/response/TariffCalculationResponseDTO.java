package com.cs205.tariffg4t2.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TariffCalculationResponseDTO {
    private String message;
    private TariffCalculationResult data;
}