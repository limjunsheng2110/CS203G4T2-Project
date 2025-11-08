# Year Field Feature Implementation

## Overview
This document describes the implementation of the year field feature for tariff rate lookups. Users can now specify a year when calculating tariffs, and the system will intelligently find the appropriate tariff rate.

## Features Implemented

### 1. Database Model Updates
- **TariffRate.java**: Replaced `date` field (String) with `year` field (Integer) to store the year of tariff rates
- **Column**: `year` in the `tariff_rates` table (Integer, nullable)

### 2. Request DTO Updates
- **TariffCalculationRequestDTO.java**: Added optional `year` field
- Users can specify a year (e.g., 2025) or leave it empty for the latest available rate

### 3. Repository Enhancements
- **TariffRateRepository.java**: Added three new query methods:
  1. `findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear()` - Exact year match
  2. `findByHsCodeAndImportingCountryCodeAndExportingCountryCodeOrderByYearDesc()` - All years sorted
  3. `findClosestYearTariffRate()` - Find closest year using ABS(year difference)

### 4. Service Layer Logic
- **TariffRateService.java**: Implemented year-aware tariff rate lookup with fallback logic
- **TariffRateCRUDService.java**: Added overloaded methods for year-based queries
- **TariffCalculatorService.java**: Updated to use `year` field instead of `date` field

### 5. Response DTO Updates
- **TariffCalculationResultDTO.java**: Replaced `date` field with `year` field in the response

### 6. Frontend Updates
- **DetailPage.jsx**: Added year input field (optional)
- **App.jsx**: Updated form data state and API calls to include year field

## Lookup Logic Priority

When a user specifies a year, the system follows this priority:

1. **Exact Match**: Try to find tariff rate for exact HS code, countries, and year
2. **Closest Year**: If exact year not found, find the tariff rate with the closest year
3. **No Year Constraint**: If no year-specific rates exist, use the latest available rate (no year filter)
4. **Scrape New Data**: If nothing found, trigger web scraping and save with the requested year

## Usage Examples

### Frontend Usage
```javascript
// User enters in DetailPage:
- Import Country: US
- Export Country: CN
- HS Code: 0101210000
- Value: 10000
- Year: 2025 (optional)
```

### API Request
```json
{
  "importingCountry": "US",
  "exportingCountry": "CN",
  "hsCode": "0101210000",
  "productValue": 10000,
  "year": 2025,
  "heads": 1,
  "weight": 100
}
```

### Database Query Flow
1. Search for rate with year = 2025
2. If not found, search for closest year (e.g., 2024 or 2026)
3. If not found, search without year constraint
4. If not found, trigger web scraping

## Benefits

1. **Historical Analysis**: Users can look up tariff rates for past years
2. **Future Planning**: Users can check rates for upcoming years if available
3. **Fallback Mechanism**: Intelligent fallback ensures users get results even if exact year isn't available
4. **Automatic Year Tagging**: Newly scraped data is automatically tagged with the requested year

## Database Schema Update

You may need to run this SQL to add the year column if it doesn't exist:

```sql
ALTER TABLE tariff_rates ADD COLUMN year INT NULL;
```

## Files Modified

### Backend
- `src/main/java/com/cs203/tariffg4t2/model/basic/TariffRate.java`
- `src/main/java/com/cs203/tariffg4t2/dto/request/TariffCalculationRequestDTO.java`
- `src/main/java/com/cs203/tariffg4t2/repository/basic/TariffRateRepository.java`
- `src/main/java/com/cs203/tariffg4t2/service/tariffLogic/TariffRateService.java`
- `src/main/java/com/cs203/tariffg4t2/service/basic/TariffRateCRUDService.java`

### Frontend
- `frontend/App.jsx`
- `frontend/components/DetailPage.jsx`

## Testing

Test scenarios:
1. ✅ Search with specific year (2025)
2. ✅ Search without year (should use latest)
3. ✅ Search with year not in database (should find closest)
4. ✅ Search with no matching data (should trigger scraping)

## Notes

- Year field is optional - if not provided, system uses original behavior
- Year is stored as Integer in database (nullable)
- Scraping automatically tags new data with the requested year
- Closest year algorithm uses absolute difference: `ABS(stored_year - requested_year)`
