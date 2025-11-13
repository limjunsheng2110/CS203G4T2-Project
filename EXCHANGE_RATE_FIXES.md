# Exchange Rate Analysis Fixes

## Issues Fixed

### 1. US-China Exchange Rate Not Working
**Problem**: The exchange rate analysis only worked for Singapore-China but not US-China.

**Root Cause**: The OpenExchangeRates API uses USD as the base currency in the free tier. When querying USD to CNY, the code wasn't properly handling the case where USD is the source currency.

**Solution**: Updated `fetchAndStoreLiveRatesUSD()` method to handle three scenarios:
- **USD to other currency**: Directly use the rate from API
- **Other currency to USD**: Calculate inverse of (USD to source currency)
- **Cross rates (non-USD pairs)**: Calculate using formula: (USD → target) / (USD → source)

```java
// Calculate and store cross rate (from -> to)
BigDecimal crossRate;
if (fromCurrency.equals("USD")) {
    // USD to target currency - directly available
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
    // ... existing logic ...
}
```

### 2. Recommended Purchase Date in the Past
**Problem**: The system was returning historical dates (when the rate was lowest in the past) instead of extrapolating future dates.

**Root Cause**: The `generateRecommendation()` method was returning `trend.minRateDate` which was a historical date, not a future prediction.

**Solution**: Completely rewrote the recommendation logic to provide future-dated predictions:

#### New Recommendation Logic:
1. **Decreasing Trend** (Favorable for buyers):
   - Calculate rate of decline using moving averages
   - Estimate optimal waiting period (2-8 weeks)
   - Return future date: `today.plusWeeks(estimatedWeeks)`
   - Example: "Consider waiting approximately 4 weeks. Projected optimal purchase: around 2025-12-11"

2. **Increasing Trend** (Unfavorable for buyers):
   - Recommend immediate purchase (1 week from today)
   - Return: `today.plusWeeks(1)`
   - Example: "Purchase within the next 1-2 weeks to avoid higher costs"

3. **Stable Trend**:
   - Recommend purchasing at convenience (2 weeks from today)
   - Return: `today.plusWeeks(2)`
   - Example: "Purchase timing is flexible - consider buying within the next 2-4 weeks"

#### New Helper Method:
```java
private int estimateWeeksUntilOptimal(List<ExchangeRate> rates, TrendAnalysisResult trend, BigDecimal currentRate) {
    // Analyzes recent 4-week period vs previous 4-week period
    // Calculates weekly rate of change
    // Estimates when rate might bottom out
    // Returns: 2-8 weeks (intelligent estimate)
}
```

### 3. Currency Mapping Not Found for Other Countries
**Problem**: The system would return "Currency mapping not found" errors for countries other than Singapore.

**Root Cause**: The `resolveCountryCodeSafely()` method was returning country codes from the database that might not match the expected format in `CurrencyCodeService`, or countries might not exist in the database at all.

**Solution**: Enhanced the country code resolution with multiple fallback strategies:

#### Improved Resolution Logic:
1. **Direct Validation**: First checks if input is already a valid 2 or 3-letter code supported by CurrencyCodeService
2. **Database Lookup**: Searches country table by ID, ISO3 code, and name
3. **Common Name Inference**: Falls back to hardcoded mappings for 40+ common country names
4. **Better Logging**: Added detailed debug logging to trace resolution process

#### New Features:
```java
private String inferCountryCode(String countryInput) {
    // Maps common country names to ISO codes:
    // "United States" / "USA" / "America" → "US"
    // "China" → "CN"
    // "United Kingdom" / "UK" / "Britain" → "GB"
    // ... 40+ country mappings
}
```

**Now Supports**:
- 2-letter ISO codes: US, CN, GB, SG, JP, CA, AU, etc.
- 3-letter ISO codes: USA, CHN, GBR, SGP, JPN, CAN, AUS, etc.
- Full country names: "United States", "China", "Singapore", etc.
- Common variations: "USA", "UK", "America", "Britain", etc.

## Testing the Fixes

### Test Case 1: US to China (USD to CNY)
```powershell
# Run the application
cd "C:\Users\simka\Documents\GitHub\Projects\CS203G4T2-Project"
.\mvnw.cmd spring-boot:run

# Test with curl or Postman
curl -X POST http://localhost:8080/api/exchange-rate/analyze `
  -H "Content-Type: application/json" `
  -d '{"exportingCountry":"US","importingCountry":"CN"}'
```

**Expected Response**:
- `exportingCurrency`: "USD"
- `importingCurrency`: "CNY"
- `currentRate`: ~7.2 (USD to CNY)
- `recommendedPurchaseDate`: Future date (e.g., "2025-11-20" or later)
- `recommendation`: Contains forward-looking advice with future date

### Test Case 2: China to US (CNY to USD)
```powershell
curl -X POST http://localhost:8080/api/exchange-rate/analyze `
  -H "Content-Type: application/json" `
  -d '{"exportingCountry":"CN","importingCountry":"US"}'
```

**Expected Response**:
- `exportingCurrency`: "CNY"
- `importingCurrency`: "USD"
- `currentRate`: ~0.139 (CNY to USD)
- `recommendedPurchaseDate`: Future date based on trend analysis

### Test Case 3: Singapore to China (SGD to CNY)
```powershell
curl -X POST http://localhost:8080/api/exchange-rate/analyze `
  -H "Content-Type: application/json" `
  -d '{"exportingCountry":"SG","importingCountry":"CN"}'
```

**Expected Response**: Should continue working as before with future dates

### Test Case 4: Various Country Input Formats
```powershell
# All of these should now work:

# Using 2-letter codes
curl -X POST http://localhost:8080/api/exchange-rate/analyze `
  -d '{"exportingCountry":"US","importingCountry":"CN"}'

# Using 3-letter codes  
curl -X POST http://localhost:8080/api/exchange-rate/analyze `
  -d '{"exportingCountry":"USA","importingCountry":"CHN"}'

# Using full country names
curl -X POST http://localhost:8080/api/exchange-rate/analyze `
  -d '{"exportingCountry":"United States","importingCountry":"China"}'

# Using common variations
curl -X POST http://localhost:8080/api/exchange-rate/analyze `
  -d '{"exportingCountry":"America","importingCountry":"China"}'
```

### Test Case 5: Other Country Pairs
```powershell
# UK to Japan
curl -X POST http://localhost:8080/api/exchange-rate/analyze `
  -d '{"exportingCountry":"UK","importingCountry":"Japan"}'

# Germany to Australia
curl -X POST http://localhost:8080/api/exchange-rate/analyze `
  -d '{"exportingCountry":"Germany","importingCountry":"Australia"}'

# Canada to Singapore
curl -X POST http://localhost:8080/api/exchange-rate/analyze `
  -d '{"exportingCountry":"CA","importingCountry":"SG"}'
```

## Supported Countries

The system now supports 40+ countries with automatic currency mapping:

### Major Economies:
- **US/USA/America** → USD
- **CN/China** → CNY
- **JP/Japan** → JPY
- **GB/UK/Britain** → GBP
- **DE/Germany** → EUR
- **FR/France** → EUR
- **CA/Canada** → CAD
- **AU/Australia** → AUD

### Asian Countries:
- **SG/Singapore** → SGD
- **HK/Hong Kong** → HKD
- **KR/South Korea** → KRW
- **IN/India** → INR
- **TH/Thailand** → THB
- **MY/Malaysia** → MYR
- **ID/Indonesia** → IDR
- **PH/Philippines** → PHP
- **VN/Vietnam** → VND

### European Countries:
- **IT/Italy** → EUR
- **ES/Spain** → EUR
- **NL/Netherlands** → EUR
- **BE/Belgium** → EUR
- **AT/Austria** → EUR
- **PT/Portugal** → EUR
- **IE/Ireland** → EUR
- **GR/Greece** → EUR
- **FI/Finland** → EUR
- **SE/Sweden** → SEK
- **NO/Norway** → NOK
- **DK/Denmark** → DKK
- **CH/Switzerland** → CHF

### Others:
- **BR/Brazil** → BRL
- **MX/Mexico** → MXN
- **RU/Russia** → RUB
- **NZ/New Zealand** → NZD

## Key Improvements

1. ✅ **USD Handling**: Properly handles USD as source or target currency
2. ✅ **Future Predictions**: Always returns dates in the future (1-8 weeks ahead)
3. ✅ **Intelligent Extrapolation**: Uses trend analysis to estimate optimal timing
4. ✅ **Better Explanations**: Detailed recommendations with current rate, historical best, and projected timing
5. ✅ **Robust Country Resolution**: Works with 2-letter codes, 3-letter codes, full names, and common variations
6. ✅ **40+ Countries Supported**: Comprehensive country and currency mapping
7. ✅ **Enhanced Logging**: Detailed debug logs help troubleshoot resolution issues

## Technical Details

### Files Modified:
- `src/main/java/com/cs203/tariffg4t2/service/basic/ExchangeRateService.java`

### Methods Updated:
1. `fetchAndStoreLiveRatesUSD()` - Fixed USD currency handling
2. `generateRecommendation()` - Rewrote to provide future dates
3. `estimateWeeksUntilOptimal()` - NEW method for intelligent waiting period calculation
4. `resolveCountryCodeSafely()` - Enhanced with validation and multiple fallback strategies
5. `inferCountryCode()` - NEW method for inferring country codes from common names

### Date Calculation Examples:
- Today: 2025-11-13
- Decreasing trend: Recommend 2025-11-27 (2 weeks) to 2025-12-25 (6 weeks)
- Increasing trend: Recommend 2025-11-20 (1 week)
- Stable trend: Recommend 2025-11-27 (2 weeks)

## Fallback Rates
The system includes fallback rates for 14+ currency pairs when the API is unavailable, ensuring continuous operation even without live data.
