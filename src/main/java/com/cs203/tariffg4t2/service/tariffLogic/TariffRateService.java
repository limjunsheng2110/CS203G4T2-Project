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
    private TariffCacheService tariffCacheService;

    @Autowired
    private WebScrapingService webScrapingService;

    @Autowired
    private TariffRateCRUDService tariffRateCRUDService;

    @Autowired
    private TariffRateRepository tariffRateRepository;

    public BigDecimal calculateTariffAmount(TariffCalculationRequestDTO request) {
        logger.debug("Calculating tariff amount for HS code: {}, importing: {}, exporting: {}",
                    request.getHsCode(), request.getImportingCountry(), request.getExportingCountry());

        // Step 1: Check if tariff rate exists in repository
        Optional<TariffRate> tariffRateOptional = tariffRateCRUDService.getTariffRateByDetails(
            request.getHsCode(),
            request.getImportingCountry(),
            request.getExportingCountry()
        );

        logger.info("Repository lookup result for HS={}, {}->{}. Found: {}",
                   request.getHsCode(),
                   request.getExportingCountry(),
                   request.getImportingCountry(),
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

                    // Step 3: Save all scraped data to repository
                    saveScrapedDataToRepository(scrapedResponse.getData());

                    // Step 4: Try to find the specific HS code again
                    tariffRateOptional = tariffRateCRUDService.getTariffRateByDetails(
                        request.getHsCode(),
                        request.getImportingCountry(),
                        request.getExportingCountry()
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
    private void saveScrapedDataToRepository(List<ScrapedTariffData> scrapedDataList) {
        int savedCount = 0;

        for (ScrapedTariffData scrapedData : scrapedDataList) {
            try {
                // Parse tariff rate from string (e.g., "7.5%" -> 7.5)
                BigDecimal adValoremRate = parseTariffRate(scrapedData.getTariffRate());

                logger.debug("Attempting to save: HS={}, importing={}, exporting={}, rate={}",
                            scrapedData.getHsCode(),
                            scrapedData.getImportingCountry(),
                            scrapedData.getExportingCountry(),
                            adValoremRate);

                // Check if this exact record already exists
                Optional<TariffRate> existing = tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                    scrapedData.getHsCode(),
                    scrapedData.getImportingCountry(),
                    scrapedData.getExportingCountry()
                );

                if (existing.isEmpty()) {
                    // Create new TariffRate entity - save exactly as scraped
                    TariffRate tariffRate = new TariffRate();
                    tariffRate.setHsCode(scrapedData.getHsCode());
                    tariffRate.setImportingCountryCode(scrapedData.getImportingCountry());
                    tariffRate.setExportingCountryCode(scrapedData.getExportingCountry());
                    tariffRate.setAdValoremRate(adValoremRate);

                    // Save to repository
                    TariffRate saved = tariffRateRepository.save(tariffRate);
                    savedCount++;

                    logger.debug("Saved tariff rate with ID={}: HS={}, importing={}, exporting={}, rate={}%",
                                saved.getId(),
                                saved.getHsCode(),
                                saved.getImportingCountryCode(),
                                saved.getExportingCountryCode(),
                                saved.getAdValoremRate());
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
}
