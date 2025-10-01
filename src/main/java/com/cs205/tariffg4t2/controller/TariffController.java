package com.cs205.tariffg4t2.controller;

import com.cs205.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.cs205.tariffg4t2.dto.response.TariffCalculationResponseDTO;
import com.cs205.tariffg4t2.dto.response.TariffCalculationResult;
import com.cs205.tariffg4t2.service.tariffLogic.TariffCalculatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/tariff")
public class TariffController {

    @Autowired
    private TariffCalculatorService tariffService;

    @GetMapping("/calculate")
    public ResponseEntity<TariffCalculationResponseDTO> calculateTariff(
            @RequestParam String importingCountry,
            @RequestParam String exportingCountry,
            @RequestParam(required = false) BigDecimal productValue,
            @RequestParam(required = false) BigDecimal quantity,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String shippingMode,
            @RequestParam(required = false) String hsCode) {

        try {
            // Create calculation request
            TariffCalculationRequestDTO request = TariffCalculationRequestDTO.builder()
                    .importingCountry(importingCountry.trim().toUpperCase())
                    .exportingCountry(exportingCountry.trim().toUpperCase())
                    .productValue(productValue)
                    .quantity(quantity)
                    .unit(unit.trim())
                    .shippingMode(shippingMode.trim())
                    .hsCode(hsCode)
                    .build();


            // Calculate tariff (validation is handled in the service layer)
            TariffCalculationResult result = tariffService.calculateTariff(request);

            return ResponseEntity.ok(new TariffCalculationResponseDTO("Success", result));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new TariffCalculationResponseDTO(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new TariffCalculationResponseDTO("Internal server error", null));
        }
    }



}