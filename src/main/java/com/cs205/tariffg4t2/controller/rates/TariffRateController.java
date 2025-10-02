package com.cs205.tariffg4t2.controller.rates;

import com.cs205.tariffg4t2.dto.basic.TariffRateDTO;
import com.cs205.tariffg4t2.model.basic.TariffRate;
import com.cs205.tariffg4t2.service.basic.TariffRateCRUDService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tariff-rates")
public class TariffRateController {

    @Autowired
    private TariffRateCRUDService tariffRateService;

    @PostMapping
    public ResponseEntity<TariffRate> createTariffRate(@RequestBody TariffRateDTO tariffRateDto) {
        TariffRate created = tariffRateService.createTariffRate(tariffRateDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TariffRate>> getAllTariffRates() {
        List<TariffRate> tariffRates = tariffRateService.getAllTariffRates();
        return ResponseEntity.ok(tariffRates);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TariffRate> getTariffRateById(@PathVariable Long id) {
        Optional<TariffRate> tariffRate = tariffRateService.getTariffRateById(id);
        return tariffRate.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<TariffRate> getTariffRateByDetails(
            @RequestParam String hsCode,
            @RequestParam String importingCountryCode,
            @RequestParam String exportingCountryCode) {
        Optional<TariffRate> tariffRate = tariffRateService.getTariffRateByDetails(
                hsCode, importingCountryCode, exportingCountryCode);
        return tariffRate.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TariffRate> updateTariffRate(
            @PathVariable Long id,
            @RequestBody TariffRateDTO tariffRateDto) {
        try {
            TariffRate updated = tariffRateService.updateTariffRate(id, tariffRateDto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTariffRate(@PathVariable Long id) {
        try {
            tariffRateService.deleteTariffRate(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
