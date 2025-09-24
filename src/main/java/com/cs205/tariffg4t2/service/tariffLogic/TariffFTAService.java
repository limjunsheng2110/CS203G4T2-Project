package com.cs205.tariffg4t2.service.tariffLogic;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TariffFTAService {

    public BigDecimal applyTradeAgreementDiscount(BigDecimal baseRate, String homeCountry, String destinationCountry) {
        // Apply trade agreement discounts
        String agreement = getApplicableTradeAgreement(homeCountry, destinationCountry);

        switch (agreement) {
            case "USMCA":
                return baseRate.multiply(BigDecimal.valueOf(0.5)); // 50% discount
            case "EU Trade Agreement":
                return baseRate.multiply(BigDecimal.valueOf(0.7)); // 30% discount
            case "WTO Most Favored Nation":
                return baseRate.multiply(BigDecimal.valueOf(0.9)); // 10% discount
            default:
                return baseRate; // No discount
        }
    }

    public String getApplicableTradeAgreement(String homeCountry, String destinationCountry) {
        // Simple trade agreement logic - expand based on real agreements
        if (isNAFTACountry(homeCountry) && isNAFTACountry(destinationCountry)) {
            return "USMCA";
        } else if (isEUCountry(homeCountry) && isEUCountry(destinationCountry)) {
            return "EU Single Market";
        } else if (isWTOMember(homeCountry) && isWTOMember(destinationCountry)) {
            return "WTO Most Favored Nation";
        }
        return "Standard Rate";
    }

    private boolean isNAFTACountry(String country) {
        return "USA".equalsIgnoreCase(country) ||
               "CAN".equalsIgnoreCase(country) ||
               "MEX".equalsIgnoreCase(country);
    }

    private boolean isEUCountry(String country) {
        // Simplified - add more EU country codes
        return "DEU".equalsIgnoreCase(country) ||
               "FRA".equalsIgnoreCase(country) ||
               "ITA".equalsIgnoreCase(country);
    }

    private boolean isWTOMember(String country) {
        // Most countries are WTO members - simplified check
        return true;
    }
}

