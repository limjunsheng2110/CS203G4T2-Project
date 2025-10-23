package com.cs203.tariffg4t2.service.tariffLogic;

import com.cs203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.cs203.tariffg4t2.model.basic.TariffRate;
import com.cs203.tariffg4t2.service.basic.PreferentialRateService;
import com.cs203.tariffg4t2.service.basic.TariffRateCRUDService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

@Service
public class TariffFTAService {

    @Autowired
    private PreferentialRateService preferentialRateService;

    @Autowired
    private TariffRateCRUDService tariffRateCRUDService;

    public BigDecimal applyTradeAgreementDiscount(TariffCalculationRequestDTO request, BigDecimal dutyAmount) {

        Optional<TariffRate> tariffRateOptional = tariffRateCRUDService.getTariffRateByDetails(
                request.getHsCode(),
                request.getImportingCountry(),
                request.getExportingCountry()
        );

        if (tariffRateOptional.isEmpty()) {
            throw new RuntimeException("No tariff rate found for the given parameters");
        }

        TariffRate tariffRate = tariffRateOptional.get();
        System.out.println("Found tariff rate for FTA: " + tariffRate);
        // Check if there's a preferential rate applicable

        // Get preferential rate if its advalorem(e.g., 0.05 for 5% duty rate)
        if (Objects.equals(tariffRate.getTariffType(), "SPECIFIC")) {
            if (preferentialRateService.getSpecificPreferentialRate(
                    request.getImportingCountry(),
                    request.getExportingCountry(),
                    request.getHsCode()
            ) == null) {
                return dutyAmount; // No preferential rate, return original duty amount
            }

            BigDecimal specificPreferentialRate = preferentialRateService.getSpecificPreferentialRate(
                    request.getImportingCountry(),
                    request.getExportingCountry(),
                    request.getHsCode()
            );
            return specificPreferentialRate.multiply(new BigDecimal(request.getHeads()));
        } else {
            // Ad Valorem case
            if (Objects.equals(tariffRate.getTariffType(), "AD_VALOREM")) {
                if (preferentialRateService.getAdValoremPreferentialRate(
                        request.getImportingCountry(),
                        request.getExportingCountry(),
                        request.getHsCode()
                ) == null) {
                    return dutyAmount; // No preferential rate, return original duty amount
                }

                BigDecimal adValoremPreferentialRate = preferentialRateService.getAdValoremPreferentialRate(
                        request.getImportingCountry(),
                        request.getExportingCountry(),
                        request.getHsCode()
                );
                return adValoremPreferentialRate.multiply(request.getProductValue());
            }
            return null;
        }
    }
}
