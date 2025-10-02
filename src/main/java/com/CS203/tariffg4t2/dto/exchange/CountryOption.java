package com.CS203.tariffg4t2.dto.exchange;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryOption {
    private String countryCode;
    private String countryName;

    // Original currency of the source country
    private String originalCurrency;
    private BigDecimal productValueInOriginalCurrency;

    // Converted to user's preferred currency
    private BigDecimal productValueConverted;
    private BigDecimal tariffAmount;
    private BigDecimal shippingCost;
    private BigDecimal totalCost;

    // Exchange rate used (originalCurrency -> preferred currency)
    private BigDecimal exchangeRate;

    // Ranking
    private int ranking;                    // 1 = cheapest, 2 = second cheapest, etc.
    private BigDecimal savingsVsExpensive;  // Saved vs most expensive option

    // Additional details
    private String tariffType;
    private boolean hasFTA;
}
