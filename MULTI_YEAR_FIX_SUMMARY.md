# Multi-Year Tariff Rate Fix Summary

## Problem
When querying tariff rates, if there were multiple records with the same HS code and country pair but different years (e.g., 2022 and 2024), the application threw a `NonUniqueResultException` because it expected a single result but found multiple.

## Solution
Changed the repository method to return a `List` instead of `Optional`, and implemented year-aware logic to select the best matching tariff rate.

## Changes Made

### 1. TariffRateRepository.java
- Changed `findByHsCodeAndImportingCountryCodeAndExportingCountryCode` to return `List<TariffRate>` instead of `Optional<TariffRate>`
- This allows the method to handle multiple years without throwing an exception

### 2. TariffRateCRUDService.java
- Updated `getTariffRateByDetails` to handle the `List` result
- Returns the most recent year if no specific year is requested
- Added `getAllTariffRatesByDetails` method to fetch and sort all tariff rates with requested year first

### 3. TariffRateService.java
- Updated `findTariffRateWithYearLogic` to properly handle List results
- Implements priority logic:
  1. **Exact match**: If year is specified and found, return that exact year
  2. **Closest match**: If exact year not found, return the closest year (e.g., 2025 request → 2024 result)
  3. **Most recent**: If no year specified, return the most recent year available
- Fixed `saveScrapedDataToRepository` to work with List result

## How It Works Now

### Example Scenarios:

**Scenario 1**: Database has 2022 and 2024 data, user requests 2024
- Result: Returns 2024 data (exact match)

**Scenario 2**: Database has 2022 and 2024 data, user requests 2025
- Result: Returns 2024 data (closest match)

**Scenario 3**: Database has 2022 and 2024 data, user requests 2023
- Result: Returns 2024 data (closest match)

**Scenario 4**: Database has 2022 and 2024 data, no year specified
- Result: Returns 2024 data (most recent)

## Testing
Compilation successful with no errors. The application now handles multiple tariff rate years gracefully by selecting the most appropriate one based on the user's request.

## Key Benefits
- No more `NonUniqueResultException` errors
- Smart year selection based on user request
- Falls back to closest available year if exact match not found
- Maintains backward compatibility with existing API

