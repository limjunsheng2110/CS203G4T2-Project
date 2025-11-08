package com.cs203.tariffg4t2.service.basic;

import com.cs203.tariffg4t2.dto.basic.ShippingRateDTO;
import com.cs203.tariffg4t2.model.basic.Country;
import com.cs203.tariffg4t2.model.basic.ShippingRate;
import com.cs203.tariffg4t2.repository.basic.CountryRepository;
import com.cs203.tariffg4t2.repository.basic.ShippingRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ShippingService {

    private final ShippingRateRepository shippingRateRepository;
    private final CountryRepository countryRepository;

    public BigDecimal getShippingRate(String shippingMode, String importingCountry, String exportingCountry) {
        List<ShippingRate> shippingRates = shippingRateRepository
                .findByImportingAndExportingCountry(importingCountry, exportingCountry);

        // If no shipping rate found or no shipping mode specified, return null
        if (shippingRates.isEmpty() || shippingMode == null) {
            return null;
        }

        // Get the first (most recent) shipping rate
        ShippingRate shippingRate = shippingRates.get(0);

        return switch (shippingMode.toUpperCase()) {
            case "AIR" -> shippingRate.getAirRate();
            case "SEA" -> shippingRate.getSeaRate();
            default -> null;
        };
    }

    public List<ShippingRateDTO> getAllShippingRates() {
        return shippingRateRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    public Optional<ShippingRateDTO> getShippingRateById(Long id) {
        return shippingRateRepository.findById(id)
                .map(this::convertToDTO);
    }

    public ShippingRateDTO createShippingRate(ShippingRateDTO dto) {
        ShippingRate shippingRate = convertToEntity(dto);
        ShippingRate saved = shippingRateRepository.save(shippingRate);
        return convertToDTO(saved);
    }

    public Optional<ShippingRateDTO> updateShippingRate(Long id, ShippingRateDTO dto) {
        return shippingRateRepository.findById(id)
                .map(existing -> {
                    updateEntityFromDTO(existing, dto);
                    ShippingRate saved = shippingRateRepository.save(existing);
                    return convertToDTO(saved);
                });
    }

    public boolean deleteShippingRate(Long id) {
        if (shippingRateRepository.existsById(id)) {
            shippingRateRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private ShippingRateDTO convertToDTO(ShippingRate entity) {
        return new ShippingRateDTO(
                entity.getId(),
                entity.getImportingCountry().getCountryCode(),
                entity.getExportingCountry().getCountryCode(),
                entity.getAirRate(),
                entity.getSeaRate()
        );
    }

    private ShippingRate convertToEntity(ShippingRateDTO dto) {
        ShippingRate entity = new ShippingRate();
        updateEntityFromDTO(entity, dto);
        return entity;
    }

    private void updateEntityFromDTO(ShippingRate entity, ShippingRateDTO dto) {
        Country importingCountry = countryRepository.findByCountryCode(dto.getImportingCountryCode())
                .orElseThrow(() -> new RuntimeException("Importing country not found: " + dto.getImportingCountryCode()));
        Country exportingCountry = countryRepository.findByCountryCode(dto.getExportingCountryCode())
                .orElseThrow(() -> new RuntimeException("Exporting country not found: " + dto.getExportingCountryCode()));

        entity.setImportingCountry(importingCountry);
        entity.setExportingCountry(exportingCountry);
        entity.setAirRate(dto.getAirRate());
        entity.setSeaRate(dto.getSeaRate());
    }

}
