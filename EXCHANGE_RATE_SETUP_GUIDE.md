# Exchange Rate Feature - Quick Setup Guide

## Prerequisites
- PostgreSQL database running
- Java 21 installed
- Node.js and npm installed
- Spring Boot application configured

## Step 1: Get OpenExchangeRates API Key

1. Visit https://openexchangerates.org/signup
2. Sign up for a FREE account (no credit card required)
3. After signup, you'll receive an App ID (API Key)
4. Free tier includes:
   - 1,000 requests/month
   - Hourly updates
   - 170+ currencies
   - Historical data

## Step 2: Configure Environment Variables

### Option A: Using .env file (Recommended for local development)

1. Create a `.env` file in the project root (if not exists)
2. Add your API key:

```bash
# Add to .env file
OPENEXCHANGERATES_API_KEY=your_actual_api_key_here

# Other required variables
DB_URL=jdbc:postgresql://localhost:5432/your_database
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password
JWT_SECRET=your_jwt_secret
```

### Option B: Using system environment variables

```bash
# Linux/Mac
export OPENEXCHANGERATES_API_KEY=your_actual_api_key_here

# Windows
set OPENEXCHANGERATES_API_KEY=your_actual_api_key_here
```

### Option C: Using IDE (IntelliJ IDEA)

1. Run → Edit Configurations
2. Select your Spring Boot application
3. Add to Environment Variables: `OPENEXCHANGERATES_API_KEY=your_actual_api_key_here`

## Step 3: Database Setup

The exchange_rate table will be created automatically by Spring Boot's Hibernate DDL auto-update.

To manually create (optional):

```sql
CREATE TABLE IF NOT EXISTS exchange_rate (
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

## Step 4: Seed Initial Data (Optional but Recommended)

To avoid making API calls immediately, you can seed some initial exchange rate data:

```sql
-- Seed some recent exchange rates (CNY to USD)
INSERT INTO exchange_rate (from_currency, to_currency, rate, rate_date, created_at, updated_at)
VALUES 
    ('CNY', 'USD', 0.1385, CURRENT_DATE, NOW(), NOW()),
    ('CNY', 'USD', 0.1382, CURRENT_DATE - INTERVAL '7 days', NOW(), NOW()),
    ('CNY', 'USD', 0.1378, CURRENT_DATE - INTERVAL '14 days', NOW(), NOW()),
    ('CNY', 'USD', 0.1375, CURRENT_DATE - INTERVAL '21 days', NOW(), NOW()),
    ('CNY', 'USD', 0.1380, CURRENT_DATE - INTERVAL '28 days', NOW(), NOW()),
    ('CNY', 'USD', 0.1383, CURRENT_DATE - INTERVAL '35 days', NOW(), NOW());

-- Seed GBP to USD
INSERT INTO exchange_rate (from_currency, to_currency, rate, rate_date, created_at, updated_at)
VALUES 
    ('GBP', 'USD', 1.2750, CURRENT_DATE, NOW(), NOW()),
    ('GBP', 'USD', 1.2720, CURRENT_DATE - INTERVAL '7 days', NOW(), NOW()),
    ('GBP', 'USD', 1.2680, CURRENT_DATE - INTERVAL '14 days', NOW(), NOW());

-- Add more as needed for your testing
```

## Step 5: Start the Backend

```bash
# Using Maven
./mvnw spring-boot:run

# Or using your IDE
# Run Tariffg4t2Application.java
```

Verify the application starts without errors and you see:
```
INFO: Started Tariffg4t2Application in X.XXX seconds
```

## Step 6: Start the Frontend

```bash
cd frontend
npm install  # First time only
npm run dev
```

The frontend should start on http://localhost:5173 (or another port shown in console)

## Step 7: Test the Feature

### Backend API Test

```bash
# Test health check
curl http://localhost:8080/api/exchange-rates/health

# Test analysis endpoint
curl -X POST http://localhost:8080/api/exchange-rates/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "importingCountry": "US",
    "exportingCountry": "CN"
  }'
```

Expected response should include:
- `currentRate`
- `averageRate`
- `recommendedPurchaseDate`
- `trendAnalysis`
- `liveDataAvailable` (true if API key works, false if using fallback)

### Frontend Test

1. Open http://localhost:5173 in your browser
2. Fill in the tariff calculation form:
   - Select "United States" as Importing Country
   - Select "China" as Exporting Country
   - Fill other required fields
3. Click "Calculate" or "Search"
4. On the Results page, you should see the "Exchange Rate Analysis" section
5. Click "Analyze Exchange Rates" button
6. You should see:
   - Current exchange rate
   - Trend analysis
   - Min/Max historical rates
   - Recommended purchase date
   - Data source indicator (Live or Fallback)

## Step 8: Verify Everything Works

### ✅ Checklist

- [ ] Backend starts without errors
- [ ] Frontend starts and loads
- [ ] Can access health endpoint: `/api/exchange-rates/health`
- [ ] Can access API reference: `/api/exchange-rates/api-reference`
- [ ] POST analysis endpoint returns valid data
- [ ] GET analysis endpoint works
- [ ] Frontend component loads on Results page
- [ ] Clicking "Analyze Exchange Rates" shows data
- [ ] Can see live data indicator (or fallback message)
- [ ] Link to OpenExchangeRates works
- [ ] Error handling works (try invalid country code)

## Troubleshooting

### Problem: "API key not configured" error

**Solution**: 
```bash
# Verify environment variable is set
echo $OPENEXCHANGERATES_API_KEY  # Linux/Mac
echo %OPENEXCHANGERATES_API_KEY%  # Windows

# If empty, set it and restart the application
```

### Problem: Always shows "fallback data" even with valid API key

**Possible causes**:
1. API key invalid - check on OpenExchangeRates dashboard
2. API rate limit exceeded - check your usage at OpenExchangeRates
3. Network connectivity issue - check internet connection
4. API URL incorrect - should be `https://openexchangerates.org/api`

**Debug**:
```bash
# Check application logs for errors
tail -f logs/application.log

# Test API key directly
curl "https://openexchangerates.org/api/latest.json?app_id=YOUR_API_KEY&base=USD&symbols=CNY"
```

### Problem: "No exchange rate data available"

**Solution**: Seed initial data as shown in Step 4, or wait for first API call to populate database.

### Problem: "Currency mapping not found"

**Solution**: The country you selected doesn't have a currency mapping. Check `CurrencyCodeService.java` and add the mapping if needed.

### Problem: Frontend shows CORS errors

**Solution**: 
1. Check `@CrossOrigin` annotation in `ExchangeRateController.java`
2. Update to allow your frontend origin:
```java
@CrossOrigin(origins = "http://localhost:5173")
```

### Problem: Tests fail

**Solution**:
```bash
# Run tests to see specific errors
./mvnw test

# Run specific test
./mvnw test -Dtest=ExchangeRateServiceTest

# Check test database configuration in application-test.properties
```

## Running Tests

```bash
# Run all tests
./mvnw test

# Run only exchange rate tests
./mvnw test -Dtest=ExchangeRate*

# Run with coverage
./mvnw test jacoco:report
```

## Production Deployment Checklist

Before deploying to production:

- [ ] Set strong `JWT_SECRET`
- [ ] Use production database credentials
- [ ] Verify `OPENEXCHANGERATES_API_KEY` is set in production environment
- [ ] Enable HTTPS
- [ ] Configure proper CORS origins
- [ ] Set up monitoring/logging
- [ ] Configure rate limiting
- [ ] Set up database backups
- [ ] Test fallback mechanism
- [ ] Document API endpoints for team
- [ ] Set up alerts for API failures

## API Rate Management

Free tier: 1,000 requests/month

**Best Practices**:
1. Cache rates for 24 hours before refreshing
2. Update all currency pairs in single batch
3. Use database fallback frequently
4. Monitor usage via OpenExchangeRates dashboard
5. Consider upgrading to paid plan for production

**Monitoring API Usage**:
- Check dashboard: https://openexchangerates.org/account
- Track in application logs
- Set up alerts when nearing limit

## Need Help?

1. **Check the detailed documentation**: `EXCHANGE_RATE_FEATURE.md`
2. **Review test cases**: They show all usage examples
3. **Check logs**: Backend logs show detailed error messages
4. **API Documentation**: https://docs.openexchangerates.org/
5. **Contact**: Reach out to the development team

## Quick Commands Reference

```bash
# Backend
./mvnw spring-boot:run                    # Start backend
./mvnw test                               # Run tests
./mvnw clean install                      # Build project

# Frontend
cd frontend
npm run dev                               # Start dev server
npm run build                             # Build for production

# Database
psql -U username -d database              # Connect to DB
\dt                                       # List tables
SELECT * FROM exchange_rate LIMIT 10;    # View rates

# Test API
curl http://localhost:8080/api/exchange-rates/health
curl -X GET "http://localhost:8080/api/exchange-rates/analyze?importingCountry=US&exportingCountry=CN"
```

## Summary

You now have a fully functional exchange rate analysis feature that:
- ✅ Fetches live exchange rates from OpenExchangeRates
- ✅ Stores historical data in PostgreSQL
- ✅ Analyzes 6-month trends
- ✅ Provides purchase recommendations
- ✅ Falls back gracefully when API is unavailable
- ✅ Displays beautifully in the React frontend
- ✅ Includes comprehensive tests

**Next Steps**: Try different country combinations and explore the trend analysis!

