package com.CS203.tariffg4t2.service.tariffLogic;

import com.CS203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.CS203.tariffg4t2.model.basic.TariffRate;
import com.CS203.tariffg4t2.service.basic.PreferentialRateService;
import com.CS203.tariffg4t2.service.basic.TariffRateCRUDService;
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

                if (Boolean.FALSE.equals(request.getRooEligible())) {
                    return dutyAmount; // no RoO => no preference
                }
  
                BigDecimal prefAdValorem = preferentialRateService.getAdValoremPreferentialRate(
                        request.getImportingCountry(), request.getExportingCountry(), request.getHsCode());
                BigDecimal prefSpecific = preferentialRateService.getSpecificPreferentialRate(
                        request.getImportingCountry(), request.getExportingCountry(), request.getHsCode());
  
                // Apply whichever leg exists matching the tariff type
                String tariffType = Optional.ofNullable(tariffRate.getTariffType()).orElse("").toUpperCase();
                if (tariffType.startsWith("AD")) {
                    return (prefAdValorem != null) ? prefAdValorem.multiply(request.getProductValue()) : dutyAmount;
                } else if (tariffType.startsWith("SPECIFIC")) {
                    BigDecimal qty = ("HEAD".equalsIgnoreCase(tariffRate.getUnitBasis()))
                            ? BigDecimal.valueOf(request.getHeads())
                            : request.getWeight();
                    return (prefSpecific != null && qty != null) ? prefSpecific.multiply(qty) : dutyAmount;
                } else if (tariffType.startsWith("COMPOUND")) {
                    BigDecimal adLeg = (prefAdValorem != null) ? prefAdValorem.multiply(request.getProductValue()) : BigDecimal.ZERO;
                    BigDecimal spLeg = (prefSpecific != null)
                            ? prefSpecific.multiply(("HEAD".equalsIgnoreCase(tariffRate.getUnitBasis()))
                            ? BigDecimal.valueOf(request.getHeads()) : request.getWeight())
                            : BigDecimal.ZERO;
                    return adLeg.add(spLeg);
                } else if (tariffType.startsWith("MIXED")) {
                    BigDecimal adLeg = (prefAdValorem != null) ? prefAdValorem.multiply(request.getProductValue()) : null;
                    BigDecimal spLeg = (prefSpecific != null)
                            ? prefSpecific.multiply(("HEAD".equalsIgnoreCase(tariffRate.getUnitBasis()))
                            ? BigDecimal.valueOf(request.getHeads()) : request.getWeight())
                            : null;
                    if (adLeg == null || spLeg == null) return dutyAmount; // incomplete pref leg
                    return tariffType.contains("MAX") ? adLeg.max(spLeg) : adLeg.min(spLeg);
                }
                return dutyAmount;
            }  
            return null;
        }
    }
}
