package com.cs203.tariffg4t2.service.tariffLogic;

import com.cs203.tariffg4t2.service.basic.ShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cs203.tariffg4t2.dto.request.TariffCalculationRequestDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ShippingCostService {

    @Autowired
    private ShippingService shippingService;

    /**
     * Calculate shipping cost using flat rates from database
     * The flat rate (e.g., $10, $15) is applied directly as the shipping cost
     * Only supports AIR and SEA modes
     */
    public BigDecimal calculateShippingCost(TariffCalculationRequestDTO request) {
        // Get flat shipping rate based on shipping mode and country pair
        BigDecimal shippingRate = shippingService.getShippingRate(
                request.getShippingMode(),
                request.getImportingCountry(),
                request.getExportingCountry()
        );

        // If no shipping rate found or no shipping mode specified, return zero
        if (shippingRate == null) {
            return BigDecimal.ZERO;
        }

        // Return the flat rate as the shipping cost
        return shippingRate.setScale(2, RoundingMode.HALF_UP);
    }
}
