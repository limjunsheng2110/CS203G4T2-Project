package com.cs203.tariffg4t2.controller;

import com.cs203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.cs203.tariffg4t2.dto.response.TariffCalculationResultDTO;
import com.cs203.tariffg4t2.service.tariffLogic.TariffCalculatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tariff")
@CrossOrigin(origins = "*")
public class TariffController {

    @Autowired
    private TariffCalculatorService tariffCalculatorService;

    // POST endpoint: JSON body matches TariffCalculationRequestDTO
    @PostMapping("/calculate")
    public ResponseEntity<TariffCalculationResultDTO> calculatePost(
            @RequestBody TariffCalculationRequestDTO request) {
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(request);
        return ResponseEntity.ok(result);
    }

    // Optional: GET endpoint for quick manual tests (maps query params into the DTO)
    // Remove this if you only want POST.
    @GetMapping("/calculate")
    public ResponseEntity<TariffCalculationResultDTO> calculateGet(
            @RequestParam("importingCountry") String importingCountry,
            @RequestParam("exportingCountry") String exportingCountry,
            @RequestParam("hsCode") String hsCode,
            @RequestParam("productValue") String productValue,
            @RequestParam("weight") String weight
    ) {
        TariffCalculationRequestDTO req = TariffCalculationRequestDTO.builder()
                .importingCountry(importingCountry)
                .exportingCountry(exportingCountry)
                .hsCode(hsCode)
                .productValue(new java.math.BigDecimal(productValue))
                .weight(new java.math.BigDecimal(weight))
                .build();

        TariffCalculationResultDTO result = tariffCalculatorService.calculate(req);
        return ResponseEntity.ok(result);
    }
    
    // Add this exception handler
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
