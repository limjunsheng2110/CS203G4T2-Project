package com.cs205.tariffg4t2.service.tariffLogic;

import com.cs205.tariffg4t2.service.basic.ShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cs205.tariffg4t2.dto.request.TariffCalculationRequestDTO;

import java.math.BigDecimal;

@Service
public class ShippingCostService {

    @Autowired
    private ShippingService shippingService;

    public BigDecimal calculateShippingCost(TariffCalculationRequestDTO request) {


        //find shippingCost using the ShippingCost Entity
        BigDecimal shippingCostRate = shippingService.getShippingRate(
                request.getShippingMode(), request.getImportingCountry(), request.getExportingCountry());
        if (shippingCostRate == null) {
            return new BigDecimal(0);
        }

        // Calculate shipping cost based on product value
        BigDecimal shippingCost = shippingCostRate.multiply(request.getQuantity());
        return shippingCost.setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}


