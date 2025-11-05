package com.cs203.tariffg4t2.service.data;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to map country codes to currency codes (ISO 4217)
 * This is a simplified version - in production, you might want to use a database table
 * or external library like java-money
 */
@Service
public class CurrencyCodeService {
    
    private static final Map<String, String> COUNTRY_TO_CURRENCY = new HashMap<>();
    
    static {
        // Major currencies
        COUNTRY_TO_CURRENCY.put("US", "USD");
        COUNTRY_TO_CURRENCY.put("USA", "USD");
        COUNTRY_TO_CURRENCY.put("GB", "GBP");
        COUNTRY_TO_CURRENCY.put("GBR", "GBP");
        COUNTRY_TO_CURRENCY.put("EU", "EUR");
        COUNTRY_TO_CURRENCY.put("JP", "JPY");
        COUNTRY_TO_CURRENCY.put("JPN", "JPY");
        COUNTRY_TO_CURRENCY.put("CN", "CNY");
        COUNTRY_TO_CURRENCY.put("CHN", "CNY");
        COUNTRY_TO_CURRENCY.put("CA", "CAD");
        COUNTRY_TO_CURRENCY.put("CAN", "CAD");
        COUNTRY_TO_CURRENCY.put("AU", "AUD");
        COUNTRY_TO_CURRENCY.put("AUS", "AUD");
        COUNTRY_TO_CURRENCY.put("NZ", "NZD");
        COUNTRY_TO_CURRENCY.put("NZL", "NZD");
        COUNTRY_TO_CURRENCY.put("CH", "CHF");
        COUNTRY_TO_CURRENCY.put("CHE", "CHF");
        COUNTRY_TO_CURRENCY.put("SG", "SGD");
        COUNTRY_TO_CURRENCY.put("SGP", "SGD");
        COUNTRY_TO_CURRENCY.put("HK", "HKD");
        COUNTRY_TO_CURRENCY.put("HKG", "HKD");
        COUNTRY_TO_CURRENCY.put("IN", "INR");
        COUNTRY_TO_CURRENCY.put("IND", "INR");
        COUNTRY_TO_CURRENCY.put("KR", "KRW");
        COUNTRY_TO_CURRENCY.put("KOR", "KRW");
        COUNTRY_TO_CURRENCY.put("BR", "BRL");
        COUNTRY_TO_CURRENCY.put("BRA", "BRL");
        COUNTRY_TO_CURRENCY.put("MX", "MXN");
        COUNTRY_TO_CURRENCY.put("MEX", "MXN");
        COUNTRY_TO_CURRENCY.put("RU", "RUB");
        COUNTRY_TO_CURRENCY.put("RUS", "RUB");
        COUNTRY_TO_CURRENCY.put("ZA", "ZAR");
        COUNTRY_TO_CURRENCY.put("ZAF", "ZAR");
        
        // European countries using EUR
        String[] euroCountries = {"DE", "DEU", "FR", "FRA", "IT", "ITA", "ES", "ESP", 
                                  "NL", "NLD", "BE", "BEL", "AT", "AUT", "PT", "PRT",
                                  "IE", "IRL", "GR", "GRC", "FI", "FIN"};
        for (String country : euroCountries) {
            COUNTRY_TO_CURRENCY.put(country, "EUR");
        }
        
        // Southeast Asian countries
        COUNTRY_TO_CURRENCY.put("MY", "MYR");
        COUNTRY_TO_CURRENCY.put("MYS", "MYR");
        COUNTRY_TO_CURRENCY.put("TH", "THB");
        COUNTRY_TO_CURRENCY.put("THA", "THB");
        COUNTRY_TO_CURRENCY.put("ID", "IDR");
        COUNTRY_TO_CURRENCY.put("IDN", "IDR");
        COUNTRY_TO_CURRENCY.put("PH", "PHP");
        COUNTRY_TO_CURRENCY.put("PHL", "PHP");
        COUNTRY_TO_CURRENCY.put("VN", "VND");
        COUNTRY_TO_CURRENCY.put("VNM", "VND");
        
        // Middle East
        COUNTRY_TO_CURRENCY.put("AE", "AED");
        COUNTRY_TO_CURRENCY.put("ARE", "AED");
        COUNTRY_TO_CURRENCY.put("SA", "SAR");
        COUNTRY_TO_CURRENCY.put("SAU", "SAR");
        COUNTRY_TO_CURRENCY.put("IL", "ILS");
        COUNTRY_TO_CURRENCY.put("ISR", "ILS");
        
        // Other regions
        COUNTRY_TO_CURRENCY.put("AR", "ARS");
        COUNTRY_TO_CURRENCY.put("ARG", "ARS");
        COUNTRY_TO_CURRENCY.put("CL", "CLP");
        COUNTRY_TO_CURRENCY.put("CHL", "CLP");
        COUNTRY_TO_CURRENCY.put("CO", "COP");
        COUNTRY_TO_CURRENCY.put("COL", "COP");
        COUNTRY_TO_CURRENCY.put("PE", "PEN");
        COUNTRY_TO_CURRENCY.put("PER", "PEN");
        COUNTRY_TO_CURRENCY.put("TR", "TRY");
        COUNTRY_TO_CURRENCY.put("TUR", "TRY");
        COUNTRY_TO_CURRENCY.put("PL", "PLN");
        COUNTRY_TO_CURRENCY.put("POL", "PLN");
        COUNTRY_TO_CURRENCY.put("SE", "SEK");
        COUNTRY_TO_CURRENCY.put("SWE", "SEK");
        COUNTRY_TO_CURRENCY.put("NO", "NOK");
        COUNTRY_TO_CURRENCY.put("NOR", "NOK");
        COUNTRY_TO_CURRENCY.put("DK", "DKK");
        COUNTRY_TO_CURRENCY.put("DNK", "DKK");
        COUNTRY_TO_CURRENCY.put("CZ", "CZK");
        COUNTRY_TO_CURRENCY.put("CZE", "CZK");
    }
    
    /**
     * Get currency code from country code (supports alpha-2 and alpha-3)
     * @param countryCode ISO 3166-1 alpha-2 or alpha-3 country code
     * @return ISO 4217 currency code or null if not found
     */
    public String getCurrencyCode(String countryCode) {
        if (countryCode == null || countryCode.isEmpty()) {
            return null;
        }
        return COUNTRY_TO_CURRENCY.get(countryCode.toUpperCase());
    }
    
    /**
     * Check if a currency code mapping exists for a country
     */
    public boolean hasCurrencyMapping(String countryCode) {
        return countryCode != null && COUNTRY_TO_CURRENCY.containsKey(countryCode.toUpperCase());
    }
}

