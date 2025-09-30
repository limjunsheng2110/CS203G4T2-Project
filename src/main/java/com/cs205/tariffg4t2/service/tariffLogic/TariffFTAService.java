package com.cs205.tariffg4t2.service.tariffLogic;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

@Service
public class TariffFTAService {

    /** Apply FTA preference. */
    public BigDecimal applyTradeAgreementDiscount(BigDecimal basePercent, 
                                                  String homeCountry, 
                                                  String destinationCountry) {
        if (basePercent == null) return null;

        String agreement = getApplicableTradeAgreement(homeCountry, destinationCountry);
        BigDecimal factor = discountFactorFor(agreement);

        BigDecimal adjusted = basePercent.multiply(factor);
        if (adjusted.compareTo(BigDecimal.ZERO) < 0) adjusted = BigDecimal.ZERO;
        return adjusted.setScale(4, RoundingMode.HALF_UP);
    }

    /**
    * Return a human-friendly agreement label used by the UI/result.
    * Keep names in sync with the switch cases in discountFactorFor().
    */
    public String getApplicableTradeAgreement(String homeCountry, String destinationCountry) {
        if (isUSMCA(homeCountry) && isUSMCA(destinationCountry)) {
            return "USMCA";
        } else if (isEUCountry(homeCountry) && isEUCountry(destinationCountry)) {
            // Intra-EU trade is effectively duty-free for most goods in practice.
            return "EU Trade Agreement";
        } else if (isWTOMember(homeCountry) && isWTOMember(destinationCountry)) {
            // MFN is the baseline schedule (no special preference/discount).
            return "WTO Most Favored Nation";
        }
        return "None";
    }

    // --- Helpers ------------------------------------------------------------

    private BigDecimal discountFactorFor(String agreement) {
        switch (agreement) {
            case "USMCA":
            // Example simplification: 50% off the base rate (ad-valorem or specific).
            return new BigDecimal(0.50);
        case "EU Trade Agreement":
            // Example simplification: 30% off.
            return new BigDecimal(0.70);
        case "WTO Most Favored Nation":
            // Example simplification: 10% off.
            return new BigDecimal(0.9);
        case "None":
        default:
            // MFN/None are baseline = no preference.
            return BigDecimal.ONE;
        }
    }

    // Region membership checks (simplified; extend as needed)
    private static Set<String> USMCA = Set.of("USA", "US", "CAN", "CA", "MEX", "MX");

    private boolean isUSMCA(String c) {
        return c != null && USMCA.contains(c.toUpperCase());
    }

    private static final Set<String> EU = Set.of(
    "DEU","DE","GER","FRA","FR","ITA","IT","ESP","ES","NLD","NL","BEL","BE","POL","PL",
    "SWE","SE","DNK","DK","AUT","AT","PRT","PT","IRL","IE","GRC","GR","FIN","FI","CZE","CZ",
    "HUN","HU","SVK","SK","SVN","SI","ROU","RO","BGR","BG","HRV","HR","LVA","LV","LTU","LT",
    "EST","EE","LUX","LU","MLT","MT","CYP","CY"
    );

    private boolean isEUCountry(String c) {
        return c != null && EU.contains(c.toUpperCase());
    }

    private boolean isWTOMember(String c) {
        // For v1 assume true for most partners; keep as-is but remember:
        // WTO MFN is the baseline schedule (no discount).
        return true;
    }
}

