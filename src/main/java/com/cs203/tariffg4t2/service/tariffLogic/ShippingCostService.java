package com.cs203.tariffg4t2.service.tariffLogic;

import com.cs203.tariffg4t2.service.basic.ShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cs203.tariffg4t2.dto.request.TariffCalculationRequestDTO;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class ShippingCostService {

    @Autowired
    private ShippingService shippingService;

    public BigDecimal calculateShippingCost(TariffCalculationRequestDTO request) {


        //find shippingCost using the ShippingCost Entity
        //air rate, sea rate, land rate
        //air rate would be something like 0.3, so if its 0.3 then it would be like 0.3 dollars per kg
        //so if its 10kg it would be 3 dollars
        //so we would need to get the shipping mode from the request
        //then we would need to get the importing country and exporting country from the request
        //then we would need to get the quantity
        //distance is baked into shipping rate, so pairs of countries further apart would have higher rates.
        BigDecimal shippingCostRate = shippingService.getShippingRate(
                request.getShippingMode(), request.getImportingCountry(), request.getExportingCountry());

        if (shippingCostRate == null) {
            return new BigDecimal(0);
        }

        if (Objects.equals(request.getShippingMode(), "LAND")) {

            //no existent of landrate, return error
            if (shippingCostRate.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("No land shipping route between " + request.getImportingCountry() + " and " + request.getExportingCountry());
            }
            BigDecimal distance = shippingService.getDistance(request.getImportingCountry(), request.getExportingCountry());
            Integer numberofHeads = request.getHeads();


            // Calculate shipping cost based on distance and weight (total Weight)
            BigDecimal shippingCost = shippingCostRate.multiply(distance).multiply(request.getTotalWeight());

            System.out.println("Distance: " + distance);
            System.out.println("Shipping Cost Rate: " + shippingCostRate);
            System.out.println("Quantity: " + request.getTotalWeight());
            System.out.println("Calculated Shipping Cost for land: " + shippingCost);

            return shippingCost.setScale(2, BigDecimal.ROUND_HALF_UP);
        }

        // Calculate shipping cost based on WEIGHT only for air and sea
        //air and sea is simpler, just rate * weight
        BigDecimal shippingCost = shippingCostRate.multiply(request.getTotalWeight());
        return shippingCost.setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}


