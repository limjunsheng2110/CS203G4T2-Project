package com.cs203.tariffg4t2.service.tariffLogic;

import com.cs203.tariffg4t2.service.basic.TariffRateCRUDService;
import com.cs203.tariffg4t2.service.data.WebScrapingService;
import com.cs203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.cs203.tariffg4t2.dto.scraping.ScrapedTariffResponse;
import com.cs203.tariffg4t2.dto.scraping.ScrapedTariffData;
import com.cs203.tariffg4t2.model.basic.TariffRate;
import com.cs203.tariffg4t2.repository.basic.TariffRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.List;

@Service
public class TariffRateService {

    private static final Logger logger = LoggerFactory.getLogger(TariffRateService.class);

    @Autowired
    private WebScrapingService webScrapingService;

    @Autowired
    private TariffRateCRUDService tariffRateCRUDService;

    @Autowired
    private TariffRateRepository tariffRateRepository;

    public BigDecimal calculateTariffAmount(TariffCalculationRequestDTO request) {
        logger.debug("Calculating tariff amount for HS code: {}, importing: {}, exporting: {}, year: {}",
                    request.getHsCode(), request.getImportingCountry(), request.getExportingCountry(), request.getYear());

        // Step 1: Check if tariff rate exists in repository (with year-aware logic)
        Optional<TariffRate> tariffRateOptional = findTariffRateWithYearLogic(
            request.getHsCode(),
            request.getImportingCountry(),
            request.getExportingCountry(),
            request.getYear()
        );

        logger.info("Repository lookup result for HS={}, {}->{}{}. Found: {}",
                   request.getHsCode(),
                   request.getExportingCountry(),
                   request.getImportingCountry(),
                   request.getYear() != null ? " (year: " + request.getYear() + ")" : "",
                   tariffRateOptional.isPresent());

        // Step 2: If not found, trigger webscraping for the country pair
        if (tariffRateOptional.isEmpty()) {
            logger.info("Tariff rate not found in repository. Triggering webscraping for {}->{}",
                       request.getExportingCountry(), request.getImportingCountry());

            try {
                // Call webscraping service
                ScrapedTariffResponse scrapedResponse = webScrapingService.scrapeTariffData(
                    request.getImportingCountry(),
                    request.getExportingCountry()
                );

                if ("success".equals(scrapedResponse.getStatus()) && scrapedResponse.getData() != null) {
                    logger.info("Successfully scraped {} tariff records. Saving to repository...",
                               scrapedResponse.getResults_count());

                    // Step 3: Save all scraped data to repository (with current year)
                    saveScrapedDataToRepository(scrapedResponse.getData(), request.getYear());

                    // Step 4: Try to find the specific HS code again
                    tariffRateOptional = findTariffRateWithYearLogic(
                        request.getHsCode(),
                        request.getImportingCountry(),
                        request.getExportingCountry(),
                        request.getYear()
                    );

                    logger.info("After scraping - Repository lookup result for HS={}, {}->{}. Found: {}",
                               request.getHsCode(),
                               request.getExportingCountry(),
                               request.getImportingCountry(),
                               tariffRateOptional.isPresent());
                } else {
                    logger.warn("Webscraping failed or returned no data for {}->{}: {}",
                               request.getExportingCountry(), request.getImportingCountry(),
                               scrapedResponse.getStatus());
                }
            } catch (Exception e) {
                logger.error("Error during webscraping for {}->{}: {}",
                            request.getExportingCountry(), request.getImportingCountry(), e.getMessage(), e);
            }
        }

        // Step 5: Calculate tariff amount if rate is found
        if (tariffRateOptional.isEmpty()) {
            logger.warn("No tariff rate found for HS code: {} after webscraping attempt", request.getHsCode());
            // Return zero instead of throwing exception - let the calculation continue
            return BigDecimal.ZERO;
        }

        TariffRate tariffRate = tariffRateOptional.get();
        logger.debug("Found tariff rate with ad valorem rate: {}", tariffRate.getAdValoremRate());

        // Calculate tariff amount based on ad valorem rate
        BigDecimal tariffAmount = BigDecimal.ZERO;

        if (tariffRate.getAdValoremRate() != null && tariffRate.getAdValoremRate().compareTo(BigDecimal.ZERO) > 0) {
            // Ad valorem calculation: rate * product value
            BigDecimal productValue = request.getProductValue() != null ? request.getProductValue() : BigDecimal.ZERO;
            tariffAmount = tariffRate.getAdValoremRate().multiply(productValue);

            //divide by 100 since all tariff rates in percentages
            tariffAmount = tariffAmount.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
            logger.debug("Ad valorem tariff amount calculated: {}", tariffAmount);
        } else {
            logger.debug("Tariff rate is zero or null, resulting in zero tariff amount.");
        }

        // Round to 2 decimal places
        tariffAmount = tariffAmount.setScale(2, RoundingMode.HALF_UP);

        logger.info("Final calculated tariff amount: {}", tariffAmount);
        return tariffAmount;
    }

    /**
     * Save all scraped tariff data to the repository
     */
    private void saveScrapedDataToRepository(List<ScrapedTariffData> scrapedDataList, Integer requestedYear) {
        int savedCount = 0;

        for (ScrapedTariffData scrapedData : scrapedDataList) {
            try {
                // Parse tariff rate from string (e.g., "7.5%" -> 7.5)
                BigDecimal adValoremRate = parseTariffRate(scrapedData.getTariffRate());

                // Extract year from the date field
                Integer year = extractYearFromDate(scrapedData.getDate(), requestedYear);

                logger.debug("Attempting to save: HS={}, importing={}, exporting={}, rate={}, year={}",
                            scrapedData.getHsCode(),
                            scrapedData.getImportingCountry(),
                            scrapedData.getExportingCountry(),
                            adValoremRate,
                            year);

                // Check if this exact record already exists
                List<TariffRate> existingList = tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                    scrapedData.getHsCode(),
                    scrapedData.getImportingCountry(),
                    scrapedData.getExportingCountry()
                );

                if (existingList.isEmpty()) {
                    // Create new TariffRate entity - save exactly as scraped
                    TariffRate tariffRate = new TariffRate();
                    tariffRate.setHsCode(scrapedData.getHsCode());
                    tariffRate.setImportingCountryCode(scrapedData.getImportingCountry());
                    tariffRate.setExportingCountryCode(scrapedData.getExportingCountry());
                    tariffRate.setAdValoremRate(adValoremRate);
                    tariffRate.setYear(year); // Set the extracted year

                    // Save to repository
                    TariffRate saved = tariffRateRepository.save(tariffRate);
                    savedCount++;

                    logger.debug("Saved tariff rate with ID={}: HS={}, importing={}, exporting={}, rate={}%, year={}",
                                saved.getId(),
                                saved.getHsCode(),
                                saved.getImportingCountryCode(),
                                saved.getExportingCountryCode(),
                                saved.getAdValoremRate(),
                                saved.getYear());
                } else {
                    logger.debug("Tariff rate already exists for HS={}, importing={}, exporting={}",
                                scrapedData.getHsCode(),
                                scrapedData.getImportingCountry(),
                                scrapedData.getExportingCountry());
                }
            } catch (Exception e) {
                logger.error("Error saving scraped data for HS={}: {}",
                            scrapedData.getHsCode(), e.getMessage(), e);
            }
        }

        logger.info("Saved {} new tariff rates to repository", savedCount);
    }

    /**
     * Extract year from date string
     * Examples: "2024" -> 2024, "2023-12-31" -> 2023, "Jan 2024" -> 2024
     * Falls back to requestedYear if parsing fails
     */
    private Integer extractYearFromDate(String dateString, Integer requestedYear) {
        if (dateString == null || dateString.trim().isEmpty()) {
            logger.debug("No date string provided, using requested year: {}", requestedYear);
            return requestedYear;
        }

        try {
            // Try to extract a 4-digit year from the string
            String yearMatch = dateString.replaceAll("[^0-9]", "");

            // Look for a 4-digit year (2000-2099)
            if (yearMatch.length() >= 4) {
                for (int i = 0; i <= yearMatch.length() - 4; i++) {
                    String potentialYear = yearMatch.substring(i, i + 4);
                    int year = Integer.parseInt(potentialYear);
                    if (year >= 2000 && year <= 2099) {
                        logger.debug("Extracted year {} from date string: {}", year, dateString);
                        return year;
                    }
                }
            }

            // If no 4-digit year found, try parsing the whole cleaned string
            if (yearMatch.length() == 4) {
                int year = Integer.parseInt(yearMatch);
                if (year >= 2000 && year <= 2099) {
                    logger.debug("Extracted year {} from date string: {}", year, dateString);
                    return year;
                }
            }

            logger.debug("Could not extract year from date string '{}', using requested year: {}", dateString, requestedYear);
            return requestedYear;

        } catch (NumberFormatException e) {
            logger.warn("Failed to parse year from date string: '{}', using requested year: {}", dateString, requestedYear);
            return requestedYear;
        }
    }

    /**
     * Parse tariff rate string to BigDecimal
     * Examples: "7.5%" -> 7.5, "0.00%" -> 0.00, "15.2%" -> 15.2
     * Store as percentage value, not decimal (will divide by 100 during calculation)
     */
    private BigDecimal parseTariffRate(String tariffRateString) {
        if (tariffRateString == null || tariffRateString.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            // Remove % sign and convert to BigDecimal
            String cleanRate = tariffRateString.trim().replace("%", "");
            return new BigDecimal(cleanRate).setScale(4, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            logger.warn("Could not parse tariff rate: '{}', defaulting to 0.00", tariffRateString);
            return BigDecimal.ZERO;
        }
    }
    public Optional<TariffRate> getTariffRate(String hsCode, String importingCountry, String exportingCountry) {
        return tariffRateCRUDService.getTariffRateByDetails(hsCode, importingCountry, exportingCountry);
    }

    public BigDecimal getAdValoremRate(String hsCode, String importingCountry, String exportingCountry) {
        Optional<TariffRate> tariffRate = getTariffRate(hsCode, importingCountry, exportingCountry);
        return tariffRate.map(TariffRate::getAdValoremRate).orElse(BigDecimal.ZERO);
    }

    /**
     * Get tariff rate with year-aware logic (public method for use by other services)
     */
    public Optional<TariffRate> getTariffRateWithYear(String hsCode, String importingCountry, String exportingCountry, Integer year) {
        return findTariffRateWithYearLogic(hsCode, importingCountry, exportingCountry, year);
    }

    /**
     * Find tariff rate using year-aware logic
     * Priority:
     * 1. Exact match for HS code, importing/exporting countries, and year
     * 2. Closest year match for HS code and importing/exporting countries
     * 3. Match without year (latest available rate)
     */
    private Optional<TariffRate> findTariffRateWithYearLogic(String hsCode, String importingCountry, String exportingCountry, Integer year) {
        // If no year specified, get the most recent one
        if (year == null) {
            List<TariffRate> results = tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                    hsCode, importingCountry, exportingCountry);

            if (!results.isEmpty()) {
                // Return most recent year
                return results.stream()
                    .sorted((a, b) -> {
                        if (a.getYear() == null && b.getYear() == null) return 0;
                        if (a.getYear() == null) return 1;
                        if (b.getYear() == null) return -1;
                        return b.getYear().compareTo(a.getYear());
                    })
                    .findFirst();
            }
            return Optional.empty();
        }

        // Try exact match with year
        Optional<TariffRate> exactMatch = tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                hsCode, importingCountry, exportingCountry, year);
        if (exactMatch.isPresent()) {
            logger.info("Found exact year match: year={}", year);
            return exactMatch;
        }

        // Try to find closest year
        logger.debug("Exact year not found, searching for closest year...");
        List<TariffRate> closestYearRates = tariffRateRepository.findClosestYearTariffRate(
                hsCode, importingCountry, exportingCountry, year);
        if (!closestYearRates.isEmpty()) {
            TariffRate closest = closestYearRates.get(0);
            logger.info("Found closest year match: requested year={}, found year={}", year, closest.getYear());
            return Optional.of(closest);
        }

        // Try match without year (any available rate)
        logger.debug("No year-specific rate found, trying without year...");
        List<TariffRate> results = tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                hsCode, importingCountry, exportingCountry);

        if (!results.isEmpty()) {
            logger.info("Found rate without year constraint");
            return Optional.of(results.get(0));
        }

        logger.debug("No tariff rate found");
        return Optional.empty();
    }

    /**
     * Get all tariff rates for a given HS code and country pair
     * Returns list sorted with requested year first, then other years in descending order
     */
    public List<TariffRate> getAllTariffRates(String hsCode, String importingCountry, String exportingCountry, Integer requestedYear) {
        return tariffRateCRUDService.getAllTariffRatesByDetails(hsCode, importingCountry, exportingCountry, requestedYear);
    }
}
