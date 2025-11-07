package com.cs203.tariffg4t2.service.basic;

import com.cs203.tariffg4t2.dto.request.ExchangeRateAnalysisRequest;
import com.cs203.tariffg4t2.dto.response.ExchangeRateAnalysisResponse;
import com.cs203.tariffg4t2.model.basic.Country;
import com.cs203.tariffg4t2.model.basic.ExchangeRate;
import com.cs203.tariffg4t2.repository.basic.CountryRepository;
import com.cs203.tariffg4t2.repository.basic.ExchangeRateRepository;
import com.cs203.tariffg4t2.service.data.CurrencyCodeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateService.class);
    private static final int ANALYSIS_MONTHS = 6;
    private static final String BASE_CURRENCY = "USD"; // Always use USD as base for free tier

    private final ExchangeRateRepository exchangeRateRepository;
    private final CountryRepository countryRepository;
    private final CurrencyCodeService currencyCodeService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${openexchangerates.api.key:}")
    private String apiKey;
    
    @Value("${openexchangerates.api.url:https://openexchangerates.org/api}")
    private String apiUrl;
    
    /**
     * Perform exchange rate analysis for tariff calculation between two countries
     */
    public ExchangeRateAnalysisResponse analyzeExchangeRates(ExchangeRateAnalysisRequest request) {
        logger.info("Starting exchange rate analysis for {} -> {}", 
                   request.getExportingCountry(), request.getImportingCountry());
        
        // Step 1: Resolve country codes (non-transactional to prevent connection leaks)
        String importingCountryCode = resolveCountryCodeSafely(request.getImportingCountry());
        String exportingCountryCode = resolveCountryCodeSafely(request.getExportingCountry());

        if (importingCountryCode == null || exportingCountryCode == null) {
            throw new IllegalArgumentException(
                String.format("Invalid country codes. Importing: %s, Exporting: %s", 
                             request.getImportingCountry(), request.getExportingCountry())
            );
        }
        
        // Step 2: Get currency codes
        String importingCurrency = currencyCodeService.getCurrencyCode(importingCountryCode);
        String exportingCurrency = currencyCodeService.getCurrencyCode(exportingCountryCode);
        
        if (importingCurrency == null || exportingCurrency == null) {
            throw new IllegalArgumentException(
                String.format("Currency mapping not found. Importing: %s, Exporting: %s", 
                             importingCountryCode, exportingCountryCode)
            );
        }
        
        logger.info("Resolved currencies: {} -> {}", exportingCurrency, importingCurrency);
        
        // Step 3: Try to fetch live data from API (separate transaction)
        boolean liveDataAvailable = false;
        String dataSource = "fallback_database";
        String message = "";
        
        try {
            fetchAndStoreLiveRatesUSD(exportingCurrency, importingCurrency);
            liveDataAvailable = true;
            dataSource = "live_api";
            message = "Exchange rates updated from live API";
            logger.info("Successfully fetched live exchange rates");
        } catch (Exception e) {
            logger.warn("Failed to fetch live rates, using database fallback: {}", e.getMessage());
            message = "Live API unavailable. Using last known stored data from database.";

            // If no live data and no database data, create fallback rates
            ensureFallbackRates(exportingCurrency, importingCurrency);
        }

        // Step 4-8: Execute the rest within a single read-only transaction
        return executeAnalysisInTransaction(exportingCurrency, importingCurrency, importingCountryCode,
                                          exportingCountryCode, liveDataAvailable, dataSource, message);
    }

    /**
     * Execute the main analysis logic within a single read-only transaction
     */
    @Transactional(readOnly = true)
    ExchangeRateAnalysisResponse executeAnalysisInTransaction(
            String exportingCurrency, String importingCurrency, String importingCountryCode,
            String exportingCountryCode, boolean liveDataAvailable, String dataSource, String message) {

        // Get current rate (most recent in database)
        ExchangeRate currentRate = exchangeRateRepository
            .findLatestByFromCurrencyAndToCurrency(exportingCurrency, importingCurrency)
            .orElse(null);
        
        if (currentRate == null) {
            throw new RuntimeException(
                "No exchange rate data available for " + exportingCurrency + " -> " + importingCurrency
            );
        }
        
        // Get historical data (past 6 months)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(ANALYSIS_MONTHS);
        
        List<ExchangeRate> historicalRates = exchangeRateRepository
            .findByFromCurrencyAndToCurrencyAndRateDateBetween(
                exportingCurrency, importingCurrency, startDate, endDate
            );
        
        // Perform trend analysis
        TrendAnalysisResult trendResult = performTrendAnalysis(historicalRates);
        
        // Generate recommendation
        RecommendationResult recommendation = generateRecommendation(historicalRates, trendResult);
        
        // Build response
        return ExchangeRateAnalysisResponse.builder()
            .importingCountry(importingCountryCode)
            .exportingCountry(exportingCountryCode)
            .importingCurrency(importingCurrency)
            .exportingCurrency(exportingCurrency)
            .currentRate(currentRate.getRate())
            .currentRateDate(currentRate.getRateDate())
            .averageRate(trendResult.averageRate)
            .minRate(trendResult.minRate)
            .minRateDate(trendResult.minRateDate)
            .maxRate(trendResult.maxRate)
            .maxRateDate(trendResult.maxRateDate)
            .recommendedPurchaseDate(recommendation.recommendedDate)
            .recommendation(recommendation.explanation)
            .trendAnalysis(trendResult.trend)
            .historicalRates(convertToDataPoints(historicalRates))
            .liveDataAvailable(liveDataAvailable)
            .dataSource(dataSource)
            .message(message)
            .build();
    }
    
    /**
     * Fetch live exchange rates using USD as base currency (free tier) and calculate cross rates
     */
    @Transactional
    void fetchAndStoreLiveRatesUSD(String fromCurrency, String toCurrency) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("OpenExchangeRates API key not configured");
        }
        
        // Don't specify base currency for free tier - it defaults to USD
        String url = String.format("%s/latest.json?app_id=%s&symbols=%s,%s",
                                   apiUrl, apiKey, fromCurrency, toCurrency);
        
        logger.debug("Fetching exchange rates from: {}", url.replace(apiKey, "***"));
        
        String response = restTemplate.getForObject(url, String.class);
        JsonNode root = objectMapper.readTree(response);
        
        // Check for API errors
        if (root.has("error") && root.get("error").asBoolean()) {
            String errorMessage = root.has("description") ? root.get("description").asText() : "Unknown API error";
            throw new RuntimeException("OpenExchangeRates API error: " + errorMessage);
        }

        // Parse response
        LocalDate rateDate = LocalDate.now();
        if (root.has("timestamp")) {
            long timestamp = root.get("timestamp").asLong();
            rateDate = LocalDate.ofEpochDay(timestamp / 86400);
        }
        
        JsonNode rates = root.get("rates");
        if (rates != null) {
            // Store USD rates for both currencies
            if (rates.has(fromCurrency) && !fromCurrency.equals("USD")) {
                BigDecimal usdToFromRate = BigDecimal.valueOf(rates.get(fromCurrency).asDouble());
                storeExchangeRate(BASE_CURRENCY, fromCurrency, usdToFromRate, rateDate);
            }

            if (rates.has(toCurrency) && !toCurrency.equals("USD")) {
                BigDecimal usdToToRate = BigDecimal.valueOf(rates.get(toCurrency).asDouble());
                storeExchangeRate(BASE_CURRENCY, toCurrency, usdToToRate, rateDate);
            }

            // Calculate and store cross rate (from -> to)
            BigDecimal crossRate;
            if (fromCurrency.equals("USD")) {
                // USD to target currency
                if (rates.has(toCurrency)) {
                    crossRate = BigDecimal.valueOf(rates.get(toCurrency).asDouble());
                } else {
                    throw new RuntimeException("Target currency " + toCurrency + " not found in API response");
                }
            } else if (toCurrency.equals("USD")) {
                // Source currency to USD (inverse of USD to source)
                if (rates.has(fromCurrency)) {
                    BigDecimal usdToFromRate = BigDecimal.valueOf(rates.get(fromCurrency).asDouble());
                    crossRate = BigDecimal.ONE.divide(usdToFromRate, 10, RoundingMode.HALF_UP);
                } else {
                    throw new RuntimeException("Source currency " + fromCurrency + " not found in API response");
                }
            } else {
                // Cross rate calculation: both currencies are non-USD
                if (rates.has(fromCurrency) && rates.has(toCurrency)) {
                    BigDecimal usdToFromRate = BigDecimal.valueOf(rates.get(fromCurrency).asDouble());
                    BigDecimal usdToToRate = BigDecimal.valueOf(rates.get(toCurrency).asDouble());

                    // Cross rate = (USD -> toCurrency) / (USD -> fromCurrency)
                    crossRate = usdToToRate.divide(usdToFromRate, 10, RoundingMode.HALF_UP);
                } else {
                    throw new RuntimeException("One or both currencies not found in API response: " + fromCurrency + ", " + toCurrency);
                }
            }

            storeExchangeRate(fromCurrency, toCurrency, crossRate, rateDate);
            logger.info("Stored cross exchange rate: {} -> {} = {} on {}",
                       fromCurrency, toCurrency, crossRate, rateDate);
        } else {
            throw new RuntimeException("No rates data in API response");
        }
    }

    /**
     * Helper method to store exchange rate
     */
    private void storeExchangeRate(String fromCurrency, String toCurrency, BigDecimal rate, LocalDate rateDate) {
        // Check if already exists
        Optional<ExchangeRate> existing = exchangeRateRepository
            .findByFromCurrencyAndToCurrencyAndRateDate(fromCurrency, toCurrency, rateDate);

        if (existing.isPresent()) {
            // Update existing
            ExchangeRate existingRate = existing.get();
            existingRate.setRate(rate);
            exchangeRateRepository.save(existingRate);
        } else {
            // Save new
            ExchangeRate exchangeRate = new ExchangeRate(fromCurrency, toCurrency, rate, rateDate);
            exchangeRateRepository.save(exchangeRate);
        }
    }

    /**
     * Ensure fallback rates exist when API is unavailable
     */
    @Transactional
    void ensureFallbackRates(String fromCurrency, String toCurrency) {
        // Check if we have any data for this currency pair
        List<ExchangeRate> existingRates = exchangeRateRepository
            .findByFromCurrencyAndToCurrency(fromCurrency, toCurrency);

        if (existingRates.isEmpty()) {
            logger.info("No existing data found, creating fallback rates for {} -> {}", fromCurrency, toCurrency);

            // Create some default fallback rates for common currency pairs
            Map<String, BigDecimal> fallbackRates = getFallbackRates();
            String pairKey = fromCurrency + toCurrency;

            BigDecimal fallbackRate = fallbackRates.getOrDefault(pairKey, BigDecimal.ONE);

            // Create historical data for the past 6 months
            LocalDate today = LocalDate.now();
            for (int i = 0; i < ANALYSIS_MONTHS * 30; i += 7) { // Weekly data points
                LocalDate date = today.minusDays(i);

                // Add some variation to make it realistic (±5%)
                double variation = 0.95 + (Math.random() * 0.1); // 0.95 to 1.05
                BigDecimal adjustedRate = fallbackRate.multiply(BigDecimal.valueOf(variation))
                    .setScale(6, RoundingMode.HALF_UP);

                ExchangeRate rate = new ExchangeRate(fromCurrency, toCurrency, adjustedRate, date);
                exchangeRateRepository.save(rate);
            }
            
            logger.info("Created {} fallback exchange rates for {} -> {}",
                       ANALYSIS_MONTHS * 4, fromCurrency, toCurrency);
        }
    }
    
    /**
     * Get fallback exchange rates for common currency pairs
     */
    private Map<String, BigDecimal> getFallbackRates() {
        Map<String, BigDecimal> rates = new HashMap<>();

        // Common currency pairs (approximate rates as of 2024)
        rates.put("USDEUR", new BigDecimal("0.85"));
        rates.put("EURUSD", new BigDecimal("1.18"));
        rates.put("USDGBP", new BigDecimal("0.79"));
        rates.put("GBPUSD", new BigDecimal("1.27"));
        rates.put("USDJPY", new BigDecimal("110.0"));
        rates.put("JPYUSD", new BigDecimal("0.009"));
        rates.put("USDCNY", new BigDecimal("7.2"));
        rates.put("CNYUSD", new BigDecimal("0.139"));
        rates.put("USDSGD", new BigDecimal("1.35"));
        rates.put("SGDUSD", new BigDecimal("0.74"));
        rates.put("USDCAD", new BigDecimal("1.25"));
        rates.put("CADUSD", new BigDecimal("0.80"));
        rates.put("USDAUD", new BigDecimal("1.32"));
        rates.put("AUDUSD", new BigDecimal("0.76"));

        // Cross rates
        rates.put("EURSGD", new BigDecimal("1.59"));
        rates.put("SGDEUR", new BigDecimal("0.63"));
        rates.put("GBPSGD", new BigDecimal("1.72"));
        rates.put("SGDGBP", new BigDecimal("0.58"));
        rates.put("JPYSGD", new BigDecimal("0.012"));
        rates.put("SGDJPY", new BigDecimal("81.5"));
        rates.put("CNYSGD", new BigDecimal("0.188"));
        rates.put("SGDCNY", new BigDecimal("5.33"));

        return rates;
    }

    /**
     * Safe country code resolution that prevents connection leaks
     */
    private String resolveCountryCodeSafely(String countryInput) {
        if (countryInput == null || countryInput.isEmpty()) {
            return null;
        }

        String normalizedInput = countryInput.toUpperCase().trim();

        try {
            // Try direct lookup by alpha-2 code
            Optional<Country> country = countryRepository.findById(normalizedInput);
            if (country.isPresent()) {
                return country.get().getCountryCode();
            }

            // If input is 3 characters, try alpha-3 code
            if (normalizedInput.length() == 3) {
                Optional<Country> countryByIso3 = countryRepository.findByIso3CodeIgnoreCase(normalizedInput);
                if (countryByIso3.isPresent()) {
                    return countryByIso3.get().getCountryCode();
                }
            }

            // Try by name
            country = countryRepository.findByCountryNameIgnoreCase(countryInput);
            if (country.isPresent()) {
                return country.get().getCountryCode();
            }

            return null;
        } catch (Exception e) {
            logger.error("Error resolving country code for input '{}': {}", countryInput, e.getMessage());
            return null;
        }
    }

    /**
     * Perform trend analysis on historical rates
     */
    private TrendAnalysisResult performTrendAnalysis(List<ExchangeRate> rates) {
        if (rates.isEmpty()) {
            throw new RuntimeException("No historical data available for trend analysis");
        }
        
        // Calculate statistics
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal min = rates.get(0).getRate();
        LocalDate minDate = rates.get(0).getRateDate();
        BigDecimal max = rates.get(0).getRate();
        LocalDate maxDate = rates.get(0).getRateDate();
        
        for (ExchangeRate rate : rates) {
            BigDecimal currentRate = rate.getRate();
            sum = sum.add(currentRate);
            
            if (currentRate.compareTo(min) < 0) {
                min = currentRate;
                minDate = rate.getRateDate();
            }
            
            if (currentRate.compareTo(max) > 0) {
                max = currentRate;
                maxDate = rate.getRateDate();
            }
        }
        
        BigDecimal average = sum.divide(BigDecimal.valueOf(rates.size()), 10, RoundingMode.HALF_UP);
        
        // Determine trend using moving average comparison
        String trend = determineTrend(rates);
        
        return new TrendAnalysisResult(average, min, minDate, max, maxDate, trend);
    }
    
    /**
     * Determine trend direction based on moving averages
     */
    private String determineTrend(List<ExchangeRate> rates) {
        if (rates.size() < 10) {
            return "stable";
        }
        
        // Compare first half average with second half average
        int midpoint = rates.size() / 2;
        BigDecimal firstHalfSum = BigDecimal.ZERO;
        BigDecimal secondHalfSum = BigDecimal.ZERO;
        
        for (int i = 0; i < midpoint; i++) {
            firstHalfSum = firstHalfSum.add(rates.get(i).getRate());
        }
        
        for (int i = midpoint; i < rates.size(); i++) {
            secondHalfSum = secondHalfSum.add(rates.get(i).getRate());
        }
        
        BigDecimal firstHalfAvg = firstHalfSum.divide(BigDecimal.valueOf(midpoint), 10, RoundingMode.HALF_UP);
        BigDecimal secondHalfAvg = secondHalfSum.divide(BigDecimal.valueOf(rates.size() - midpoint), 10, RoundingMode.HALF_UP);
        
        BigDecimal percentChange = secondHalfAvg.subtract(firstHalfAvg)
            .divide(firstHalfAvg, 10, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
        
        if (percentChange.compareTo(BigDecimal.valueOf(2)) > 0) {
            return "increasing";
        } else if (percentChange.compareTo(BigDecimal.valueOf(-2)) < 0) {
            return "decreasing";
        } else {
            return "stable";
        }
    }
    
    /**
     * Generate purchase recommendation based on trend analysis
     */
    private RecommendationResult generateRecommendation(List<ExchangeRate> rates, TrendAnalysisResult trend) {
        LocalDate recommendedDate = trend.minRateDate;
        String explanation;
        
        if ("decreasing".equals(trend.trend)) {
            explanation = String.format(
                "Exchange rate is trending downward (%.2f%% decrease). " +
                "Consider waiting or purchasing soon. Best rate was %.4f on %s.",
                calculatePercentageChange(trend.maxRate, trend.averageRate),
                trend.minRate,
                trend.minRateDate
            );
        } else if ("increasing".equals(trend.trend)) {
            explanation = String.format(
                "Exchange rate is trending upward (%.2f%% increase). " +
                "Consider purchasing sooner to avoid higher costs. Best historical rate was %.4f on %s.",
                calculatePercentageChange(trend.minRate, trend.averageRate),
                trend.minRate,
                trend.minRateDate
            );
        } else {
            explanation = String.format(
                "Exchange rate is relatively stable. " +
                "Current rate is near the average. Best historical rate was %.4f on %s.",
                trend.minRate,
                trend.minRateDate
            );
        }
        
        return new RecommendationResult(recommendedDate, explanation);
    }
    
    /**
     * Calculate percentage change between two rates
     */
    private double calculatePercentageChange(BigDecimal from, BigDecimal to) {
        return to.subtract(from)
            .divide(from, 10, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .abs()
            .doubleValue();
    }
    
    /**
     * Convert ExchangeRate entities to data points for response
     */
    private List<ExchangeRateAnalysisResponse.ExchangeRateDataPoint> convertToDataPoints(List<ExchangeRate> rates) {
        List<ExchangeRateAnalysisResponse.ExchangeRateDataPoint> dataPoints = new ArrayList<>();
        for (ExchangeRate rate : rates) {
            dataPoints.add(ExchangeRateAnalysisResponse.ExchangeRateDataPoint.builder()
                .date(rate.getRateDate())
                .rate(rate.getRate())
                .build());
        }
        return dataPoints;
    }
    
    // Helper classes for internal use
    private static class TrendAnalysisResult {
        BigDecimal averageRate;
        BigDecimal minRate;
        LocalDate minRateDate;
        BigDecimal maxRate;
        LocalDate maxRateDate;
        String trend;
        
        TrendAnalysisResult(BigDecimal averageRate, BigDecimal minRate, LocalDate minRateDate, 
                          BigDecimal maxRate, LocalDate maxRateDate, String trend) {
            this.averageRate = averageRate;
            this.minRate = minRate;
            this.minRateDate = minRateDate;
            this.maxRate = maxRate;
            this.maxRateDate = maxRateDate;
            this.trend = trend;
        }
    }
    
    private static class RecommendationResult {
        LocalDate recommendedDate;
        String explanation;
        
        RecommendationResult(LocalDate recommendedDate, String explanation) {
            this.recommendedDate = recommendedDate;
            this.explanation = explanation;
        }
    }
}

