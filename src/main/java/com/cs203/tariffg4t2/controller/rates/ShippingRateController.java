package com.cs203.tariffg4t2.controller.rates;

import com.cs203.tariffg4t2.dto.basic.ShippingRateDTO;
import com.cs203.tariffg4t2.service.basic.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipping-rates")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ShippingRateController {

    private final ShippingService shippingService;

    /**
     * Get all shipping rates
     */
    @GetMapping
    public ResponseEntity<List<ShippingRateDTO>> getAllShippingRates() {
        List<ShippingRateDTO> rates = shippingService.getAllShippingRates();
        return ResponseEntity.ok(rates);
    }

    /**
     * Get shipping rate by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ShippingRateDTO> getShippingRateById(@PathVariable Long id) {
        return shippingService.getShippingRateById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new shipping rate
     */
    @PostMapping
    public ResponseEntity<ShippingRateDTO> createShippingRate(@RequestBody ShippingRateDTO dto) {
        try {
            ShippingRateDTO created = shippingService.createShippingRate(dto);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update existing shipping rate
     */
    @PutMapping("/{id}")
    public ResponseEntity<ShippingRateDTO> updateShippingRate(
            @PathVariable Long id,
            @RequestBody ShippingRateDTO dto) {
        return shippingService.updateShippingRate(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete shipping rate
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShippingRate(@PathVariable Long id) {
        boolean deleted = shippingService.deleteShippingRate(id);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
