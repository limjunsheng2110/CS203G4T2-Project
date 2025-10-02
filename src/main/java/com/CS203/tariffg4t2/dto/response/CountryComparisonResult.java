package com.CS203.tariffg4t2.dto.response;

import com.CS203.tariffg4t2.dto.exchange.CountryOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryComparisonResult {
    // Overall recommendation
    private String recommendedCountry;
    private String recommendedCountryName;
    private BigDecimal lowestTotalCost;

    // Currency used for comparison
    private String currency;

    // Detailed breakdown for each country
    private List<CountryOption> countryOptions;

    // Product / destination info
    private String productName;
    private String destinationCountry;
}
