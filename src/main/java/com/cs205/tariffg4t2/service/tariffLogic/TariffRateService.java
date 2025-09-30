package com.cs205.tariffg4t2.service.tariffLogic;

import com.cs205.tariffg4t2.service.basic.TariffRateCRUDService;
import com.cs205.tariffg4t2.service.data.WebScrapingService;
import com.cs205.tariffg4t2.dto.request.TariffCalculationRequest;
import com.cs205.tariffg4t2.model.basic.TariffRate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class TariffRateService {

    @Autowired
    private TariffCacheService tariffCacheService;

    @Autowired
    private WebScrapingService webScrapingService;

    @Autowired
    private TariffRateCRUDService tariffRateCRUDService;

    public BigDecimal calculateTariffAmount(TariffCalculationRequest request) {
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

        // Calculate based on tariff type
        if (tariffRate.getTariffType() == TariffRate.TariffType.AD_VALOREM) {
            // Ad Valorem: rate * product value
            BigDecimal rate = tariffRate.getAdValoremRate();
            if (rate == null) {
                throw new RuntimeException("Ad valorem rate is null");
            }
            return rate.multiply(request.getProductValue());
        } else if (tariffRate.getTariffType() == TariffRate.TariffType.SPECIFIC) {
            // Specific: specific rate amount per unit * quantity
            BigDecimal ratePerUnit = tariffRate.getSpecificRateAmount();
            if (ratePerUnit == null || request.getQuantity() == null) {
                throw new RuntimeException("Specific rate amount or quantity is null");
            }
            return ratePerUnit.multiply(request.getQuantity());
        } else {
            throw new RuntimeException("Unknown tariff type: " + tariffRate.getTariffType());
        }
    }
}
