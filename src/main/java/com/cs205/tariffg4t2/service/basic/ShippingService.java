package com.cs205.tariffg4t2.service.basic;

import com.cs205.tariffg4t2.model.basic.ShippingRate;
import com.cs205.tariffg4t2.repository.basic.ShippingRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class ShippingService {

    private final ShippingRateRepository shippingRateRepository;

    public BigDecimal getShippingRate(String shippingMode, String importingCountry, String exportingCountry) {
        ShippingRate shippingRate = shippingRateRepository
                .findByImportingAndExportingCountry(importingCountry, exportingCountry)
                .orElse(null);

        if (shippingRate == null) {
            return null;
        }

        return switch (shippingMode.toUpperCase()) {
            case "AIR" -> shippingRate.getAirRate();
            case "SEA" -> shippingRate.getSeaRate();
            case "LAND" -> shippingRate.getLandRate();
            default -> null;
        };
    }
}

