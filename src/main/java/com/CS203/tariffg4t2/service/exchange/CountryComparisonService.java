package com.CS203.tariffg4t2.service.exchange;

import com.CS203.tariffg4t2.dto.request.CountryComparisonRequest;
import com.CS203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.CS203.tariffg4t2.dto.response.CountryComparisonResult;
import com.CS203.tariffg4t2.dto.exchange.CountryOption;
import com.CS203.tariffg4t2.dto.response.TariffCalculationResultDTO;
import com.CS203.tariffg4t2.model.basic.Country;
import com.CS203.tariffg4t2.repository.basic.CountryRepository;
import com.CS203.tariffg4t2.service.exchange.ExchangeRateService;
import com.CS203.tariffg4t2.service.tariffLogic.TariffCalculatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class CountryComparisonService {
    
    @Autowired
    private TariffCalculatorService tariffCalculatorService;
    
    @Autowired
    private ExchangeRateService exchangeRateService;
    
    @Autowired
    private CountryRepository countryRepository;
    
    // Map of country codes to their currencies
    private static final Map<String, String> COUNTRY_CURRENCIES = Map.ofEntries(
        Map.entry("USA", "USD"),
        Map.entry("CHN", "CNY"),
        Map.entry("JPN", "JPY"),
        Map.entry("SGP", "SGD"),
        Map.entry("GBR", "GBP"),
        Map.entry("EUR", "EUR"),
        Map.entry("DEU", "EUR"),
        Map.entry("FRA", "EUR"),
        Map.entry("ITA", "EUR"),
        Map.entry("KOR", "KRW"),
        Map.entry("AUS", "AUD"),
        Map.entry("CAN", "CAD"),
        Map.entry("IND", "INR"),
        Map.entry("BRA", "BRL"),
        Map.entry("MEX", "MXN")
    );
    
    /**
     * Compare costs of buying from multiple countries
     */
    public CountryComparisonResult compareCountries(CountryComparisonRequest request) {
        List<CountryOption> options = new ArrayList<>();
        
        String preferredCurrency = request.getPreferredCurrency() != null 
            ? request.getPreferredCurrency() 
            : getCurrencyForCountry(request.getDestinationCountry());
        
        // Calculate cost for each source country
        for (String sourceCountry : request.getSourceCountries()) {
            CountryOption option = calculateCountryOption(
                sourceCountry,
                request.getDestinationCountry(),
                request,
                preferredCurrency
            );
            options.add(option);
        }
        
        // Sort by total cost (ascending)
        options.sort(Comparator.comparing(CountryOption::getTotalCost));
        
        // Assign rankings and calculate savings
        BigDecimal mostExpensiveCost = options.get(options.size() - 1).getTotalCost();
        for (int i = 0; i < options.size(); i++) {
            CountryOption option = options.get(i);
            option.setRanking(i + 1);
            
            BigDecimal savings = mostExpensiveCost.subtract(option.getTotalCost());
            option.setSavingsVsExpensive(savings);
        }
        
        // Build result
        CountryOption cheapest = options.get(0);
        
        return CountryComparisonResult.builder()
            .recommendedCountry(cheapest.getCountryCode())
            .recommendedCountryName(cheapest.getCountryName())
            .lowestTotalCost(cheapest.getTotalCost())
            .currency(preferredCurrency)
            .countryOptions(options)
            .productName(request.getProductName())
            .destinationCountry(request.getDestinationCountry())
            .build();
    }
    
    /**
     * Calculate cost for a specific source country
     */
    private CountryOption calculateCountryOption(
            String sourceCountry,
            String destinationCountry,
            CountryComparisonRequest request,
            String preferredCurrency) {
        
        // Build tariff calculation request
        TariffCalculationRequestDTO tariffRequest = TariffCalculationRequestDTO.builder()
            .importingCountry(destinationCountry)
            .exportingCountry(sourceCountry)
            .hsCode(request.getHsCode())
            .productValue(request.getProductValue())
            .weight(request.getQuantity())
            .heads(1)
            .shippingMode(request.getShippingMode())
            .build();
        
        // Calculate tariff
        TariffCalculationResultDTO tariffResult = tariffCalculatorService.calculateTariff(tariffRequest);
        
        // Get source country currency
        String sourceCurrency = getCurrencyForCountry(sourceCountry);
        
        // Get exchange rate
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(
            sourceCurrency, 
            preferredCurrency
        );
        
        // Convert all costs to preferred currency
        BigDecimal productValueConverted = request.getProductValue() != null
            ? exchangeRateService.convertCurrency(request.getProductValue(), sourceCurrency, preferredCurrency)
            : BigDecimal.ZERO;
        
        BigDecimal tariffAmountConverted = exchangeRateService.convertCurrency(
            tariffResult.getTariffAmount(), 
            sourceCurrency, 
            preferredCurrency
        );
        
        BigDecimal shippingCostConverted = tariffResult.getShippingCost() != null
            ? exchangeRateService.convertCurrency(tariffResult.getShippingCost(), sourceCurrency, preferredCurrency)
            : BigDecimal.ZERO;
        
        BigDecimal totalCostConverted = exchangeRateService.convertCurrency(
            tariffResult.getTotalCost(), 
            sourceCurrency, 
            preferredCurrency
        );
        
        // Get country name
        String countryName = getCountryName(sourceCountry);
        
        // Build option
        return CountryOption.builder()
            .countryCode(sourceCountry)
            .countryName(countryName)
            .originalCurrency(sourceCurrency)
            .productValueInOriginalCurrency(request.getProductValue())
            .productValueConverted(productValueConverted)
            .tariffAmount(tariffAmountConverted)
            .shippingCost(shippingCostConverted)
            .totalCost(totalCostConverted)
            .exchangeRate(exchangeRate)
            .tariffType(tariffResult.getTariffType())
            .hasFTA(false) // You can enhance this based on your FTA logic
            .build();
    }
    
    /**
     * Get currency for a country code
     */
    private String getCurrencyForCountry(String countryCode) {
        return COUNTRY_CURRENCIES.getOrDefault(countryCode.toUpperCase(), "USD");
    }
    
    /**
     * Get country name from code
     */
    private String getCountryName(String countryCode) {
        return countryRepository.findById(countryCode)
            .map(Country::getCountryName)
            .orElse(countryCode);
    }
}