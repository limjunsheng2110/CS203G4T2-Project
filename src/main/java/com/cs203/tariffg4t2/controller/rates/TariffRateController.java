package com.cs203.tariffg4t2.controller.rates;

import com.cs203.tariffg4t2.dto.basic.TariffRateDTO;
import com.cs203.tariffg4t2.model.basic.TariffRate;
import com.cs203.tariffg4t2.service.basic.TariffRateCRUDService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tariff-rates")
public class TariffRateController {

    private static final Logger logger = LoggerFactory.getLogger(TariffRateController.class);

    @Autowired
    private TariffRateCRUDService tariffRateService;

    @PostMapping
    public ResponseEntity<TariffRate> createTariffRate(@RequestBody TariffRateDTO tariffRateDto) {
        logger.info("Creating tariff rate: {}", tariffRateDto);
        TariffRate created = tariffRateService.createTariffRate(tariffRateDto);
        logger.info("Created tariff rate with ID: {}", created.getId());
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TariffRate>> getAllTariffRates() {
        logger.info("Fetching all tariff rates");
        List<TariffRate> tariffRates = tariffRateService.getAllTariffRates();
        logger.info("Retrieved {} tariff rates", tariffRates.size());
        return ResponseEntity.ok(tariffRates);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TariffRate> getTariffRateById(@PathVariable Long id) {
        logger.info("Fetching tariff rate by ID: {}", id);
        Optional<TariffRate> tariffRate = tariffRateService.getTariffRateById(id);
        return tariffRate.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<TariffRate> getTariffRateByDetails(
            @RequestParam String hsCode,
            @RequestParam String importingCountryCode,
            @RequestParam String exportingCountryCode) {
        logger.info("Searching tariff rate by details - HS Code: {}, Importing Country Code: {}, Exporting Country Code: {}",
                hsCode, importingCountryCode, exportingCountryCode);
        Optional<TariffRate> tariffRate = tariffRateService.getTariffRateByDetails(
                hsCode, importingCountryCode, exportingCountryCode);
        return tariffRate.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TariffRate> updateTariffRate(@PathVariable Long id, @RequestBody TariffRateDTO tariffRateDto) {
        logger.info("Updating tariff rate ID: {} with data: {}", id, tariffRateDto);
        try {
            TariffRate updated = tariffRateService.updateTariffRate(id, tariffRateDto);
            logger.info("Successfully updated tariff rate ID: {}", id);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            logger.error("Error updating tariff rate ID: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTariffRate(@PathVariable Long id) {
        logger.info("Deleting tariff rate ID: {}", id);
        try {
            tariffRateService.deleteTariffRate(id);
            logger.info("Successfully deleted tariff rate ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.error("Error deleting tariff rate ID: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }
}
