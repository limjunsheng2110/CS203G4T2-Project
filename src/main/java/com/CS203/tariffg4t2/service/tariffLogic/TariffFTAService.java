package com.CS203.tariffg4t2.service.tariffLogic;

import com.CS203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.CS203.tariffg4t2.service.basic.PreferentialRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TariffFTAService {

    @Autowired
    private PreferentialRateService preferentialRateService;

    public BigDecimal applyTradeAgreementDiscount(TariffCalculationRequestDTO request, BigDecimal dutyAmount) {

        // Get preferential rate (e.g., 0.05 for 5% duty rate)
            BigDecimal preferentialRate = preferentialRateService.getPreferentialRate(
                    request.getImportingCountry(),
                    request.getExportingCountry(),
                    request.getHsCode()
            );

            return preferentialRate;
        }
    }
