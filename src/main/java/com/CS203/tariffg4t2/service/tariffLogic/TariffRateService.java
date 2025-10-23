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
            BigDecimal ratePerUnit = tariffRate.getSpecificRateAmount();
            if (ratePerUnit == null) {
                throw new RuntimeException("Specific rate amount is null");
            }
            String unit = tariffRate.getUnitBasis(); // "HEAD" or "KG"
            if ("HEAD".equalsIgnoreCase(unit)) {
                if (request.getHeads() == null) throw new RuntimeException("Heads not provided");
                return ratePerUnit.multiply(BigDecimal.valueOf(request.getHeads()));
            } else if ("KG".equalsIgnoreCase(unit)) {
                if (request.getWeight() == null) throw new RuntimeException("Weight not provided");
                return ratePerUnit.multiply(request.getWeight());
            } else {
                throw new RuntimeException("Unknown unit basis for specific duty: " + unit);
            }
        } else if ("COMPOUND".equalsIgnoreCase(tariffType)) {
            // (percent of customs value) + (specific per unit)
            BigDecimal percent = Optional.ofNullable(tariffRate.getCompoundPercent()).orElse(BigDecimal.ZERO);
            BigDecimal specific = Optional.ofNullable(tariffRate.getCompoundSpecific()).orElse(BigDecimal.ZERO);
            BigDecimal adValoremLeg = percent.multiply(request.getProductValue()); // customs value fixed later in Calculator
            BigDecimal specificLeg;
            String unit = tariffRate.getUnitBasis();
            if ("HEAD".equalsIgnoreCase(unit)) {
                specificLeg = specific.multiply(BigDecimal.valueOf(request.getHeads()));
            } else {
                specificLeg = specific.multiply(request.getWeight());
            }
            return adValoremLeg.add(specificLeg);
        } else if ("MIXED_MAX".equalsIgnoreCase(tariffType) || "MIXED_MIN".equalsIgnoreCase(tariffType)) {
            BigDecimal percent = Optional.ofNullable(tariffRate.getMixedPercent()).orElse(BigDecimal.ZERO);
            BigDecimal specific = Optional.ofNullable(tariffRate.getMixedSpecific()).orElse(BigDecimal.ZERO);
            BigDecimal percentLeg = percent.multiply(request.getProductValue());
            BigDecimal specificLeg;
            String unit = tariffRate.getUnitBasis();
            if ("HEAD".equalsIgnoreCase(unit)) {
                specificLeg = specific.multiply(BigDecimal.valueOf(request.getHeads()));
            } else {
                specificLeg = specific.multiply(request.getWeight());
            }
            return "MIXED_MAX".equalsIgnoreCase(tariffType) ? percentLeg.max(specificLeg) : percentLeg.min(specificLeg);
        } else {
            System.out.println("Unknown tariff type: " + tariffType);
            throw new RuntimeException("Unknown tariff type: " + tariffRate.getTariffType());
        }
    }
}
