package com.CS203.tariffg4t2.service.tariffLogic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.CS203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.CS203.tariffg4t2.service.basic.PreferentialRateService;

import java.math.BigDecimal;

@Service
public class TariffFTAService {

    @Autowired
    private PreferentialRateService preferentialRateService;

    public BigDecimal applyTradeAgreementDiscount(TariffCalculationRequestDTO request, BigDecimal dutyAmount) {

        BigDecimal preferentialRate = preferentialRateService.getPreferentialRate(
                request.getImportingCountry(),
                request.getExportingCountry(),
                request.getHsCode()
        );

        // Apply preferential rate discount
        return preferentialRate;
    }

}
