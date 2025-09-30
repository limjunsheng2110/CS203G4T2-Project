package com.cs205.tariffg4t2.service.tariffLogic;

import org.springframework.stereotype.Service;

import com.cs205.tariffg4t2.dto.request.TariffCalculationRequest;

import java.math.BigDecimal;

@Service
public class ShippingCostService {

    public BigDecimal calculateShippingCost(TariffCalculationRequest request) {
        BigDecimal shippingMultiplier;
        BigDecimal shippingAmount;

        // Determine shipping multiplier based on mode
        switch (request.getShippingMode()) {
            case "air":
                shippingMultiplier = BigDecimal.valueOf(1.5); // Air shipping multiplier
                break;
            case "land":
                shippingMultiplier = BigDecimal.valueOf(1.2); // Land shipping multiplier
                break;
            case "sea":
                shippingMultiplier = BigDecimal.valueOf(1.0); // Sea shipping multiplier
                break;
            default:
                shippingMultiplier = BigDecimal.valueOf(1.5); // default to Air shipping multiplier
                request.setShippingMode("air");
        }

        shippingAmount = request.getQuantity();

        // Calculate and return the shipping cost based on the mode
        return shippingAmount.multiply(shippingMultiplier).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}


