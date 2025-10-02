package com.CS203.tariffg4t2.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class CountryComparisonRequest {
    // List of source countries to compare (e.g., ["CHN", "USA", "JPN"])
    private List<String> sourceCountries;

    // Destination country (buyer's country)
    private String destinationCountry;

    // User's preferred currency for comparison (e.g., "SGD")
    private String preferredCurrency;

    // Product details
    private String productName;

    // Keep as String to preserve leading zeros
    private String hsCode;

    // Either productValue OR (quantity + unit) can be provided
    private BigDecimal productValue;
    private BigDecimal quantity;
    private String unit;

    // Shipping details
    private String shippingMode;
}
