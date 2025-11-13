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
     * Calculate shipping cost based on weight and per-kg rate from database
     * Formula: shippingCost = ratePerKg * weight
     * The rate (e.g., $5/kg for AIR, $2/kg for SEA) is multiplied by the weight
     * Only supports AIR and SEA modes
     */
    public BigDecimal calculateShippingCost(TariffCalculationRequestDTO request) {
        // Get per-kg shipping rate based on shipping mode and country pair
        BigDecimal ratePerKg = shippingService.getShippingRate(
                request.getShippingMode(),
                request.getImportingCountry(),
                request.getExportingCountry()
        );

        // If no shipping rate found or no shipping mode specified, return zero
        if (ratePerKg == null) {
            return BigDecimal.ZERO;
        }

        // Get weight from request (in kg)
        BigDecimal weight = request.getWeight();

        // If no weight specified, return zero
        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Calculate shipping cost: rate per kg * weight
        BigDecimal shippingCost = ratePerKg.multiply(weight);

        // Return the calculated shipping cost
        return shippingCost.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Get the shipping rate per kg from database
     * @param request The tariff calculation request containing shipping mode and countries
     * @return The rate per kilogram, or zero if not found
     */
    public BigDecimal getShippingRatePerKg(TariffCalculationRequestDTO request) {
        // Get per-kg shipping rate based on shipping mode and country pair
        BigDecimal ratePerKg = shippingService.getShippingRate(
                request.getShippingMode(),
                request.getImportingCountry(),
                request.getExportingCountry()
        );

        // If no shipping rate found or no shipping mode specified, return zero
        if (ratePerKg == null) {
            return BigDecimal.ZERO;
        }

        return ratePerKg.setScale(2, RoundingMode.HALF_UP);
    }
}
