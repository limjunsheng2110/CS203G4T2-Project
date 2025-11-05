# Exchange Rate Analysis Feature

## Overview
This feature allows business owners to survey tariff rates by exchange rate analysis, helping them identify the best time to make purchases through currency trend analysis.

## User Story
**As a business owner**, I want to be able to survey my options in tariff rates by exchange rate analysis **so that** I can identify the best time to make purchases.

## Acceptance Criteria ✅
- ✅ Results page displays tariff rates for both the importing and exporting countries, using current exchange data
- ✅ System analyzes past 6 months of exchange rate trends to generate a "Recommended Purchase Date"
- ✅ A live exchange rate link (https://openexchangerates.org/) is accessible for users to validate data accuracy
- ✅ If live exchange rate API is down, a fallback message is shown using last known stored data

## Implementation Details

### Backend (Spring Boot)

#### 1. Database Model
**File**: `src/main/java/com/cs203/tariffg4t2/model/basic/ExchangeRate.java`

```java
@Entity
@Table(name = "exchange_rate")
public class ExchangeRate {
    private Long id;
    private String fromCurrency;  // ISO 4217 currency code (e.g., USD, CNY)
    private String toCurrency;    // ISO 4217 currency code
    private BigDecimal rate;      // Exchange rate value
    private LocalDate rateDate;   // Date of the exchange rate
    // timestamps: createdAt, updatedAt
}
```

#### 2. Repository Layer
**File**: `src/main/java/com/cs203/tariffg4t2/repository/basic/ExchangeRateRepository.java`

Key methods:
- `findByFromCurrencyAndToCurrencyAndRateDate()` - Find specific rate
- `findLatestByFromCurrencyAndToCurrency()` - Get most recent rate
- `findByFromCurrencyAndToCurrencyAndRateDateBetween()` - Get historical data

#### 3. Service Layer
**File**: `src/main/java/com/cs203/tariffg4t2/service/basic/ExchangeRateService.java`

Key features:
- **API Integration**: Fetches live data from OpenExchangeRates API
- **Database Caching**: Stores rates for fallback and historical analysis
- **Trend Analysis**: Analyzes 6 months of data using moving averages
- **Smart Recommendations**: Recommends optimal purchase dates based on trends

Trend Detection Algorithm:
```
1. Split historical data into two halves
2. Calculate average for each half
3. Compare to determine trend direction:
   - >2% increase → "increasing" (rates going up, purchase sooner)
   - <-2% decrease → "decreasing" (rates going down, consider waiting)
   - Otherwise → "stable"
```

#### 4. Currency Code Mapping
**File**: `src/main/java/com/cs203/tariffg4t2/service/data/CurrencyCodeService.java`

Maps country codes to currency codes (ISO 4217):
- Supports major currencies (USD, EUR, GBP, JPY, CNY, etc.)
- Covers 50+ countries
- Handles both alpha-2 and alpha-3 country codes

#### 5. REST API Endpoints
**File**: `src/main/java/com/cs203/tariffg4t2/controller/ExchangeRateController.java`

Endpoints:
- `POST /api/exchange-rates/analyze` - Main analysis endpoint
- `GET /api/exchange-rates/analyze` - Query parameter version
- `GET /api/exchange-rates/health` - Health check
- `GET /api/exchange-rates/api-reference` - Returns OpenExchangeRates link

#### 6. DTOs
Request:
```java
ExchangeRateAnalysisRequest {
    String importingCountry;  // Accepts alpha-2, alpha-3, or full name
    String exportingCountry;
}
```

Response:
```java
ExchangeRateAnalysisResponse {
    // Basic Info
    String importingCountry, exportingCountry;
    String importingCurrency, exportingCurrency;
    
    // Current Rate
    BigDecimal currentRate;
    LocalDate currentRateDate;
    
    // Trend Analysis
    BigDecimal averageRate, minRate, maxRate;
    LocalDate minRateDate, maxRateDate;
    String trendAnalysis;  // "increasing", "decreasing", "stable"
    
    // Recommendation
    LocalDate recommendedPurchaseDate;
    String recommendation;
    
    // Historical Data
    List<ExchangeRateDataPoint> historicalRates;
    
    // API Status
    boolean liveDataAvailable;
    String dataSource;  // "live_api" or "fallback_database"
    String message;
}
```

### Frontend (React)

#### Component: ExchangeRateAnalysis
**File**: `frontend/components/ExchangeRateAnalysis.jsx`

Features:
- ✅ **Input Validation**: Validates country selection before API call
- ✅ **Error Handling**: Clear error messages for invalid inputs
- ✅ **Live/Fallback Indicator**: Shows data source status with visual indicators
- ✅ **API Reference Link**: Direct link to OpenExchangeRates for validation
- ✅ **Trend Visualization**: 
  - Color-coded trend indicators (green=decreasing, red=increasing, blue=stable)
  - Simple bar chart showing historical rates
  - Min/Max rate highlights
- ✅ **Purchase Recommendation**: Prominent display with explanation
- ✅ **Responsive Design**: Works on mobile and desktop

#### Integration
**File**: `frontend/components/ResultsPage.jsx`

The component is integrated into the results page, appearing above the tariff calculation results.

### Configuration

#### Environment Variables
Add to your `.env` file or environment:

```bash
# OpenExchangeRates API Key
# Sign up at: https://openexchangerates.org/signup
# Free tier: 1,000 requests/month
OPENEXCHANGERATES_API_KEY=your_api_key_here
```

#### Application Properties
**File**: `src/main/resources/application.properties`

```properties
# Exchange Rates API
openexchangerates.api.key=${OPENEXCHANGERATES_API_KEY:}
openexchangerates.api.url=https://openexchangerates.org/api
```

## Testing

### Unit Tests
**File**: `src/test/java/com/cs203/tariffg4t2/service/ExchangeRateServiceTest.java`

Covers:
- ✅ Successful analysis with valid inputs
- ✅ Invalid country code handling
- ✅ Missing currency mapping handling
- ✅ No historical data scenario
- ✅ Alpha-3 country code support
- ✅ Trend analysis (increasing/decreasing/stable)

### Integration Tests
**File**: `src/test/java/com/cs203/tariffg4t2/integration/ExchangeRateIntegrationTest.java`

Tests full pipeline:
- ✅ POST endpoint with valid data
- ✅ GET endpoint with query parameters
- ✅ Invalid country error handling
- ✅ Missing country validation
- ✅ Alpha-3 country code support
- ✅ Different currency pairs
- ✅ Health check endpoint
- ✅ API reference endpoint
- ✅ Complete user flow (input → analysis → recommendation → output)

## Usage

### Backend API

#### Example Request
```bash
curl -X POST http://localhost:8080/api/exchange-rates/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "importingCountry": "US",
    "exportingCountry": "CN"
  }'
```

#### Example Response
```json
{
  "importingCountry": "US",
  "exportingCountry": "CN",
  "importingCurrency": "USD",
  "exportingCurrency": "CNY",
  "currentRate": 0.1385,
  "currentRateDate": "2025-11-05",
  "averageRate": 0.1378,
  "minRate": 0.1365,
  "minRateDate": "2025-09-15",
  "maxRate": 0.1392,
  "maxRateDate": "2025-10-20",
  "recommendedPurchaseDate": "2025-09-15",
  "recommendation": "Exchange rate is relatively stable. Current rate is near the average. Best historical rate was 0.1365 on 2025-09-15.",
  "trendAnalysis": "stable",
  "historicalRates": [
    { "date": "2025-05-05", "rate": 0.1370 },
    { "date": "2025-05-12", "rate": 0.1372 },
    // ... more data points
  ],
  "liveDataAvailable": true,
  "dataSource": "live_api",
  "message": "Exchange rates updated from live API"
}
```

### Frontend Usage

The feature automatically appears on the Results page when users:
1. Select importing and exporting countries
2. Submit the tariff calculation form
3. Navigate to the results page

Users can click "Analyze Exchange Rates" to:
- View current exchange rate
- See 6-month trend analysis
- Get purchase date recommendations
- Validate data on OpenExchangeRates website

## Database Schema

```sql
CREATE TABLE exchange_rate (
    id BIGSERIAL PRIMARY KEY,
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(20, 10) NOT NULL,
    rate_date DATE NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(from_currency, to_currency, rate_date)
);

CREATE INDEX idx_exchange_rate_currencies ON exchange_rate(from_currency, to_currency);
CREATE INDEX idx_exchange_rate_date ON exchange_rate(rate_date);
```

## API Rate Limits

### OpenExchangeRates Free Tier
- 1,000 requests per month
- Updates once per hour
- Historical data available
- No credit card required

### Recommendations
1. **Cache aggressively**: Store rates in database for 24 hours
2. **Batch requests**: Update all currency pairs at once
3. **Fallback gracefully**: Always use database fallback
4. **Monitor usage**: Track API calls to avoid limits

## Error Handling

### Backend
1. **Invalid Country**: Returns 400 with clear error message
2. **API Down**: Falls back to database, sets `liveDataAvailable=false`
3. **No Historical Data**: Returns 500 with descriptive message
4. **Currency Not Supported**: Returns 400 with list of supported currencies

### Frontend
1. **Network Errors**: Shows user-friendly error message
2. **Invalid Response**: Displays generic error with retry option
3. **Loading States**: Shows spinner during API calls
4. **Empty States**: Displays instructions when no data

## Future Enhancements

### Potential Improvements
1. **Real-time Updates**: WebSocket for live rate updates
2. **Email Alerts**: Notify when rates reach target thresholds
3. **Multiple Currency Pairs**: Compare multiple routes simultaneously
4. **Advanced Charting**: Interactive charts with zoom/pan
5. **Machine Learning**: Predictive modeling for future rates
6. **Currency Converter**: Built-in conversion calculator
7. **Historical Analysis**: Compare with previous years
8. **Custom Time Ranges**: Allow users to specify analysis periods

## Troubleshooting

### Common Issues

**Issue**: "Currency mapping not found"
- **Solution**: Ensure country has currency mapping in `CurrencyCodeService`
- **Solution**: Add missing mapping to the service

**Issue**: "No exchange rate data available"
- **Solution**: Verify database has historical data
- **Solution**: Check API key is valid
- **Solution**: Manually seed database with initial rates

**Issue**: API returns 401 Unauthorized
- **Solution**: Check `OPENEXCHANGERATES_API_KEY` is set correctly
- **Solution**: Verify API key is active on OpenExchangeRates dashboard

**Issue**: Frontend shows "fallback data" even with valid API key
- **Solution**: Check backend logs for API errors
- **Solution**: Verify network connectivity to OpenExchangeRates
- **Solution**: Check API rate limits haven't been exceeded

## Dependencies

### Backend
- Spring Boot 3.5.5
- Spring Data JPA
- PostgreSQL
- Jackson (JSON processing)
- Lombok
- Spring Validation

### Frontend
- React 18.2.0
- Lucide React (icons)
- Tailwind CSS

## Security Considerations

1. **API Key Security**: Store in environment variables, never commit to git
2. **Rate Limiting**: Implement request throttling to prevent abuse
3. **Input Validation**: Sanitize all user inputs
4. **SQL Injection**: Use parameterized queries (handled by Spring Data JPA)
5. **CORS**: Configure appropriate CORS policies for production

## Performance Optimization

1. **Database Indexing**: Indexes on currency pairs and dates
2. **Caching**: Store frequently accessed rates in memory
3. **Batch Processing**: Update multiple rates in single transaction
4. **Connection Pooling**: HikariCP for database connections
5. **Lazy Loading**: Load historical data only when needed

## Monitoring and Metrics

### Key Metrics to Track
- API success/failure rate
- Response times
- Cache hit ratio
- Database query performance
- User engagement with feature

### Logging
- INFO: Successful analyses, API calls
- WARN: Fallback to database, missing mappings
- ERROR: API failures, invalid inputs

## Contributors
- Implementation: CS203 G4T2 Team
- Feature Owner: Business Requirements Team
- API Provider: OpenExchangeRates.org

## License
Internal project - CS203 G4T2

## Support
For issues or questions:
1. Check this documentation
2. Review test cases for examples
3. Check backend logs
4. Contact the development team

