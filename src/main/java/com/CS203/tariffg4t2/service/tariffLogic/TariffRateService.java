package com.CS203.tariffg4t2.service.tariffLogic;

import com.CS203.tariffg4t2.service.basic.TariffRateCRUDService;
import com.CS203.tariffg4t2.service.data.WebScrapingService;
import com.CS203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.CS203.tariffg4t2.model.basic.TariffRate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

@Service
public class TariffRateService {


    //This needs s
    @Autowired
    private TariffCacheService tariffCacheService;

    @Autowired
    private WebScrapingService webScrapingService;

    @Autowired
    private TariffRateCRUDService tariffRateCRUDService;

    public BigDecimal calculateTariffAmount(TariffCalculationRequestDTO request) {
        // Get the tariff rate entity based on request parameters
        Optional<TariffRate> tariffRateOptional = tariffRateCRUDService.getTariffRateByDetails(
            request.getHsCode(),
            request.getImportingCountry(),
            request.getExportingCountry()
        );

        if (tariffRateOptional.isEmpty()) {
            throw new RuntimeException("No tariff rate found for the given parameters");
        }

        TariffRate tariffRate = tariffRateOptional.get();
        System.out.println("Found tariff rate: " + tariffRate);

        System.out.println("Calculating tariff for request: " + request);
        System.out.println("Using tariff rate: " + tariffRate.getTariffType());

        String tariffType = tariffRate.getTariffType();
        String AD_VALOREM = "AD_VALOREM";
        String SPECIFIC = "SPECIFIC";

        // Calculate based on tariff type
        if (tariffType.equals(AD_VALOREM)) {
            // Ad Valorem: rate * product value
            BigDecimal rate = tariffRate.getAdValoremRate();
            if (rate == null) {
                throw new RuntimeException("Ad valorem rate is null");
            }
            return rate.multiply(request.getProductValue());
            // Note: rate is expected to be in decimal form (e.g., 0.05 for 5%)
        } else if ("SPECIFIC".equals(tariffType)) {
            // Specific: specific rate amount per unit * quantity
            BigDecimal ratePerUnit = tariffRate.getSpecificRateAmount();
            if (ratePerUnit == null || request.getTotalWeight() == null) {
                throw new RuntimeException("Specific rate amount or quantity is null");
            }

            //calculation : just multiply rate per unit by weight and return
            return ratePerUnit.multiply((BigDecimal.valueOf(request.getHeads())));
        } else {
            System.out.println("Unknown tariff type: " + tariffType);
            throw new RuntimeException("Unknown tariff type: " + tariffRate.getTariffType());
        }
    }
}
