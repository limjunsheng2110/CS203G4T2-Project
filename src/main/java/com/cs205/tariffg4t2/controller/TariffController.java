package com.cs205.tariffg4t2.controller;

import com.cs205.tariffg4t2.dto.request.TariffCalculationRequest;
import com.cs205.tariffg4t2.dto.response.TariffCalculationResponse;
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
    public ResponseEntity<TariffCalculationResponse> calculateTariff(
            @RequestParam String homeCountry,
            @RequestParam String destinationCountry,
            @RequestParam String productName,
            @RequestParam BigDecimal productValue,
            @RequestParam BigDecimal quantity,
            @RequestParam String unit,
            @RequestParam(required = false) String shippingRateType,
            @RequestParam(required = false) BigDecimal shippingRate,
            @RequestParam(required = false) String shippingMode,
            @RequestParam(required = false) String hsCode,
            @RequestParam(required = false) String tradeAgreement) {

        try {
            // Create calculation request
            TariffCalculationRequest request = TariffCalculationRequest.builder()
                    .homeCountry(homeCountry.trim().toUpperCase())
                    .destinationCountry(destinationCountry.trim().toUpperCase())
                    .productName(productName.trim())
                    .productValue(productValue)
                    .quantity(quantity)
                    .unit(unit.trim())
                    .shippingRateType(shippingRateType != null ? TariffCalculationRequest.ShippingRateType.valueOf(shippingRateType.trim().toUpperCase()) : null)
                    .shippingRate(shippingRate)
                    .shippingMode(shippingMode != null ? TariffCalculationRequest.ShippingMode.valueOf(shippingMode.trim().toUpperCase()) : null)
                    .hsCode(hsCode)
                    .tradeAgreement(tradeAgreement)
                    .build();


            // Calculate tariff (validation is handled in the service layer)
            TariffCalculationResult result = tariffService.calculateTariff(request);

            return ResponseEntity.ok(new TariffCalculationResponse("Success", result));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new TariffCalculationResponse(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new TariffCalculationResponse("Internal server error", null));
        }
    }



}