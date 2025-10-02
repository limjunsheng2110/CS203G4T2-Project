package com.CS203.tariffg4t2.controller.rates;

import com.CS203.tariffg4t2.dto.basic.ShippingRateDTO;
import com.CS203.tariffg4t2.service.basic.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/shipping-rates")
@RequiredArgsConstructor
public class ShippingRateController {

    private final ShippingService shippingService;

    @GetMapping("/{id}")
    public ResponseEntity<ShippingRateDTO> getShippingRateById(@PathVariable Long id) {
        return shippingService.getShippingRateById(id)
                .map(rate -> ResponseEntity.ok(rate))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ShippingRateDTO> createShippingRate(@RequestBody ShippingRateDTO dto) {
        try {
            ShippingRateDTO created = shippingService.createShippingRate(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShippingRateDTO> updateShippingRate(@PathVariable Long id, @RequestBody ShippingRateDTO dto) {
        try {
            return shippingService.updateShippingRate(id, dto)
                    .map(updated -> ResponseEntity.ok(updated))
                    .orElse(ResponseEntity.notFound().build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShippingRate(@PathVariable Long id) {
        if (shippingService.deleteShippingRate(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/rate")
    public ResponseEntity<BigDecimal> getShippingRate(
            @RequestParam String shippingMode,
            @RequestParam String importingCountry,
            @RequestParam String exportingCountry) {
        BigDecimal rate = shippingService.getShippingRate(shippingMode, importingCountry, exportingCountry);
        if (rate != null) {
            return ResponseEntity.ok(rate);
        }
        return ResponseEntity.notFound().build();
    }
}
