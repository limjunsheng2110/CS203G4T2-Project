package com.cs205.tariffg4t2.service.tariffLogic;

import com.cs205.tariffg4t2.dto.TradeAgreementDTO;
import com.cs205.tariffg4t2.dto.request.TariffCalculationRequest;
import com.cs205.tariffg4t2.model.basic.TradeAgreement;
import com.cs205.tariffg4t2.service.basic.TradeAgreementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class TariffFTAService {

    @Autowired
    private TradeAgreementService tradeAgreementService;

    public BigDecimal applyTradeAgreementDiscount(TariffCalculationRequest request, BigDecimal dutyAmount) {
        if (dutyAmount == null) {
            return BigDecimal.ZERO;
        }

        // Check for applicable trade agreement

        Optional<TradeAgreement> tradeAgreement = tradeAgreementService.findApplicableTradeAgreement(request.getHomeCountry(), request.getDestinationCountry());

        if (tradeAgreement.isEmpty()) {
            return dutyAmount; // No agreement found, return original duty amount
        }

        // Apply discount based on agreement
        String agreementName = tradeAgreement.get().getName();

        BigDecimal discountFactor = getDiscountFactorForAgreement(agreementName);

        BigDecimal discountedDuty = dutyAmount.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);

        return discountedDuty;
    }

    private BigDecimal getDiscountFactorForAgreement(String agreementName) {
        if (agreementName == null) {
            return BigDecimal.ONE; // No discount
        }

        String normalizedName = agreementName.toUpperCase().trim();

        // MFN/WTO - base 10% discount (90% of original)
        if (isMFNAgreement(normalizedName)) {
            return new BigDecimal("0.90"); // 10% discount
        }

        // Specific trade agreements with higher discounts
        if (isEUAgreement(normalizedName)) {
            return new BigDecimal("0.70"); // 30% discount
        }

        if (isUSMCAAgreement(normalizedName)) {
            return new BigDecimal("0.50"); // 50% discount
        }

        if (isCPTPPAgreement(normalizedName)) {
            return new BigDecimal("0.60"); // 40% discount
        }

        if (isASEANAgreement(normalizedName)) {
            return new BigDecimal("0.75"); // 25% discount
        }

        // Default: MFN rates (10% discount)
        return new BigDecimal("0.90");
    }

    private boolean isMFNAgreement(String agreementName) {
        return agreementName.contains("MFN") ||
               agreementName.contains("MOST FAVORED NATION") ||
               agreementName.contains("WTO");
    }

    private boolean isEUAgreement(String agreementName) {
        return agreementName.contains("EU") ||
               agreementName.contains("EUROPEAN UNION") ||
               agreementName.contains("EUROPE");
    }

    private boolean isUSMCAAgreement(String agreementName) {
        return agreementName.contains("USMCA") ||
               agreementName.contains("UNITED STATES MEXICO CANADA") ||
               agreementName.contains("NAFTA");
    }

    private boolean isCPTPPAgreement(String agreementName) {
        return agreementName.contains("CPTPP") ||
               agreementName.contains("COMPREHENSIVE PROGRESSIVE") ||
               agreementName.contains("TRANS-PACIFIC");
    }

    private boolean isASEANAgreement(String agreementName) {
        return agreementName.contains("ASEAN") ||
               agreementName.contains("ASSOCIATION OF SOUTHEAST");
    }
}
