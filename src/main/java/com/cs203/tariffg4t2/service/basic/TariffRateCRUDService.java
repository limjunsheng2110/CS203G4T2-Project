package com.cs203.tariffg4t2.service.basic;

import com.cs203.tariffg4t2.dto.basic.TariffRateDTO;
import com.cs203.tariffg4t2.model.basic.TariffRate;
import com.cs203.tariffg4t2.repository.basic.TariffRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cs203.tariffg4t2.repository.basic.CountryRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class TariffRateCRUDService {

    private static final Logger logger = LoggerFactory.getLogger(TariffRateCRUDService.class);

    @Autowired
    private TariffRateRepository tariffRateRepository;

    @Autowired
    private CountryRepository countryRepository;

    public TariffRate createTariffRate(TariffRateDTO tariffRateDto) {
        TariffRate tariffRate = convertDtoToEntity(tariffRateDto);
        return tariffRateRepository.save(tariffRate);
    }

    public List<TariffRate> getAllTariffRates() {
        return tariffRateRepository.findAll();
    }

    public Optional<TariffRate> getTariffRateById(Long id) {
        return tariffRateRepository.findById(id);
    }

    public Optional<TariffRate> getTariffRateByDetails(String hsCode, String importingCountryCode, String exportingCountryCode) {
        logger.debug("Looking up tariff rate for HS: {}, importing: {}, exporting: {}",
                hsCode, importingCountryCode, exportingCountryCode);

        List<TariffRate> results = tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                hsCode, importingCountryCode, exportingCountryCode);

        if (!results.isEmpty()) {
            // Return the most recent year or first result
            TariffRate result = results.stream()
                .sorted((a, b) -> {
                    if (a.getYear() == null && b.getYear() == null) return 0;
                    if (a.getYear() == null) return 1;
                    if (b.getYear() == null) return -1;
                    return b.getYear().compareTo(a.getYear()); // Most recent first
                })
                .findFirst()
                .orElse(results.get(0));

            logger.debug("Found tariff rate with ID: {}, year: {}", result.getId(), result.getYear());
            return Optional.of(result);
        } else {
            logger.debug("No tariff rate found for given parameters");
        }

        return Optional.empty();
    }

    /**
     * Get all tariff rates by details (for multiple years)
     * Returns list sorted by year with requested year first, then other years
     */
    public List<TariffRate> getAllTariffRatesByDetails(String hsCode, String importingCountryCode, String exportingCountryCode, Integer requestedYear) {
        logger.debug("Looking up all tariff rates for HS: {}, importing: {}, exporting: {}, requested year: {}",
                hsCode, importingCountryCode, exportingCountryCode, requestedYear);

        List<TariffRate> results = tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                hsCode, importingCountryCode, exportingCountryCode);

        if (results.isEmpty()) {
            logger.debug("No tariff rates found for given parameters");
            return results;
        }

        // Sort results: requested year first, then by year descending
        results.sort((a, b) -> {
            Integer yearA = a.getYear();
            Integer yearB = b.getYear();

            // Requested year gets priority
            if (requestedYear != null) {
                boolean aIsRequested = requestedYear.equals(yearA);
                boolean bIsRequested = requestedYear.equals(yearB);

                if (aIsRequested && !bIsRequested) return -1;
                if (!aIsRequested && bIsRequested) return 1;
            }

            // Otherwise sort by year descending (most recent first)
            if (yearA == null && yearB == null) return 0;
            if (yearA == null) return 1;
            if (yearB == null) return -1;
            return yearB.compareTo(yearA);
        });

        logger.debug("Found {} tariff rate(s)", results.size());
        return results;
    }

    /**
     * Get tariff rate by details with year-aware logic
     * If year is specified, try exact match first, then find closest year
     * If year is null, use the original method
     */
    public Optional<TariffRate> getTariffRateByDetails(String hsCode, String importingCountryCode, String exportingCountryCode, Integer year) {
        if (year == null) {
            // No year specified, use original method
            return getTariffRateByDetails(hsCode, importingCountryCode, exportingCountryCode);
        }

        logger.debug("Looking up tariff rate for HS: {}, importing: {}, exporting: {}, year: {}",
                hsCode, importingCountryCode, exportingCountryCode, year);

        // Try exact match with year
        Optional<TariffRate> exactMatch = tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                hsCode, importingCountryCode, exportingCountryCode, year);

        if (exactMatch.isPresent()) {
            logger.debug("Found exact year match with ID: {}", exactMatch.get().getId());
            return exactMatch;
        }

        // Try to find closest year
        logger.debug("Exact year not found, searching for closest year...");
        List<TariffRate> closestYearRates = tariffRateRepository.findClosestYearTariffRate(
                hsCode, importingCountryCode, exportingCountryCode, year);

        if (!closestYearRates.isEmpty()) {
            TariffRate closest = closestYearRates.get(0);
            logger.info("Found closest year match: requested year={}, found year={}, ID={}",
                    year, closest.getYear(), closest.getId());
            return Optional.of(closest);
        }

        logger.debug("No tariff rate found for any year");
        return Optional.empty();
    }

    /**
     * Get tariff rate by HS code only (broad match)
     */
    public Optional<TariffRate> getTariffRateByHsCode(String hsCode) {
        logger.debug("Looking up tariff rate by HS code only: {}", hsCode);
        List<TariffRate> rates = tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeOrderByYearDesc(
                hsCode, null, null);

        if (!rates.isEmpty()) {
            logger.debug("Found tariff rate with ID: {}", rates.get(0).getId());
            return Optional.of(rates.get(0));
        }

        return Optional.empty();
    }

    public void deleteTariffRate(Long id) {
        logger.debug("Deleting tariff rate with ID: {}", id);
        if (!tariffRateRepository.existsById(id)) {
            throw new RuntimeException("TariffRate not found with id: " + id);
        }
        tariffRateRepository.deleteById(id);
    }

    public TariffRate updateTariffRate(Long id, TariffRateDTO tariffRateDto) {
        logger.debug("Updating tariff rate with ID: {}", id);
        logger.debug("Received DTO: hsCode={}, importingCountryCode={}, exportingCountryCode={}, baseRate={}",
                    tariffRateDto.getHsCode(),
                    tariffRateDto.getImportingCountryCode(),
                    tariffRateDto.getExportingCountryCode(),
                    tariffRateDto.getBaseRate());

        TariffRate existingRate = tariffRateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TariffRate not found with id: " + id));

        logger.debug("Existing rate before update: hsCode={}, importingCountryCode={}, exportingCountryCode={}, adValoremRate={}, year={}",
                    existingRate.getHsCode(),
                    existingRate.getImportingCountryCode(),
                    existingRate.getExportingCountryCode(),
                    existingRate.getAdValoremRate(),
                    existingRate.getYear());

        // Validate and update fields
        if (tariffRateDto.getHsCode() != null && !tariffRateDto.getHsCode().trim().isEmpty()) {
            existingRate.setHsCode(tariffRateDto.getHsCode().trim());
        }

        if (tariffRateDto.getImportingCountryCode() != null && !tariffRateDto.getImportingCountryCode().trim().isEmpty()) {
            String importingCode = tariffRateDto.getImportingCountryCode().trim().toUpperCase();
            if (!countryRepository.existsByCountryCodeIgnoreCase(importingCode)) {
                throw new IllegalArgumentException("Importing Country Code does not exist: " + importingCode);
            }
            existingRate.setImportingCountryCode(importingCode);
        }

        if (tariffRateDto.getExportingCountryCode() != null && !tariffRateDto.getExportingCountryCode().trim().isEmpty()) {
            String exportingCode = tariffRateDto.getExportingCountryCode().trim().toUpperCase();
            if (!countryRepository.existsByCountryCodeIgnoreCase(exportingCode)) {
                throw new IllegalArgumentException("Exporting Country Code does not exist: " + exportingCode);
            }
            existingRate.setExportingCountryCode(exportingCode);
        }

        if (tariffRateDto.getBaseRate() != null) {
            if (tariffRateDto.getBaseRate().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Base Rate cannot be negative");
            }
            existingRate.setAdValoremRate(tariffRateDto.getBaseRate());
            logger.debug("Updated adValoremRate to: {}", tariffRateDto.getBaseRate());
        }

        // Update year if provided
        if (tariffRateDto.getYear() != null) {
            existingRate.setYear(tariffRateDto.getYear());
            logger.debug("Updated year to: {}", tariffRateDto.getYear());
        }

        TariffRate savedRate = tariffRateRepository.save(existingRate);
        logger.debug("Saved rate: hsCode={}, importingCountryCode={}, exportingCountryCode={}, adValoremRate={}, year={}",
                    savedRate.getHsCode(),
                    savedRate.getImportingCountryCode(),
                    savedRate.getExportingCountryCode(),
                    savedRate.getAdValoremRate(),
                    savedRate.getYear());

        return savedRate;
    }

    private TariffRate convertDtoToEntity(TariffRateDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("TariffRateDTO cannot be null");
        }

        // Validate required fields
        if (dto.getHsCode() == null || dto.getHsCode().trim().isEmpty()) {
            throw new IllegalArgumentException("HS Code is required and cannot be empty");
        }

        if (dto.getImportingCountryCode() == null || dto.getImportingCountryCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Importing Country Code is required and cannot be empty");
        }

        if (dto.getExportingCountryCode() == null || dto.getExportingCountryCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Exporting Country Code is required and cannot be empty");
        }

        // Validate numeric fields
        if (dto.getBaseRate() != null && dto.getBaseRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Base Rate cannot be negative");
        }

        // Validate country code format (assuming 2-3 character codes)
        if (dto.getImportingCountryCode().length() < 2 || dto.getImportingCountryCode().length() > 3) {
            throw new IllegalArgumentException("Importing Country Code must be 2-3 characters long");
        }

        if (dto.getExportingCountryCode().length() < 2 || dto.getExportingCountryCode().length() > 3) {
            throw new IllegalArgumentException("Exporting Country Code must be 2-3 characters long");
        }

        if (!countryRepository.existsByCountryCodeIgnoreCase(dto.getImportingCountryCode().trim().toUpperCase())) {
            throw new IllegalArgumentException("Importing Country Code does not exist: " + dto.getImportingCountryCode());
        }

        if (!countryRepository.existsByCountryCodeIgnoreCase(dto.getExportingCountryCode().trim().toUpperCase())) {
            throw new IllegalArgumentException("Exporting Country Code does not exist: " + dto.getExportingCountryCode());
        }

        TariffRate entity = new TariffRate();
        entity.setHsCode(dto.getHsCode().trim());
        entity.setImportingCountryCode(dto.getImportingCountryCode().trim().toUpperCase());
        entity.setExportingCountryCode(dto.getExportingCountryCode().trim().toUpperCase());
        entity.setAdValoremRate(dto.getBaseRate());
        return entity;
    }
}
