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

        Optional<TariffRate> result = tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                hsCode, importingCountryCode, exportingCountryCode);

        if (result.isPresent()) {
            logger.debug("Found tariff rate with ID: {}", result.get().getId());
        } else {
            logger.debug("No tariff rate found for given parameters");
        }

        return result;
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
        logger.debug("Received DTO: hsCode={}, importingCountryCode={}, exportingCountryCode={}, baseRate={}, date={}",
                    tariffRateDto.getHsCode(),
                    tariffRateDto.getImportingCountryCode(),
                    tariffRateDto.getExportingCountryCode(),
                    tariffRateDto.getBaseRate(),
                    tariffRateDto.getDate());

        TariffRate existingRate = tariffRateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TariffRate not found with id: " + id));

        logger.debug("Existing rate before update: hsCode={}, importingCountryCode={}, exportingCountryCode={}, adValoremRate={}, date={}",
                    existingRate.getHsCode(),
                    existingRate.getImportingCountryCode(),
                    existingRate.getExportingCountryCode(),
                    existingRate.getAdValoremRate(),
                    existingRate.getDate());

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

        if (tariffRateDto.getDate() != null && !tariffRateDto.getDate().trim().isEmpty()) {
            existingRate.setDate(tariffRateDto.getDate().trim());
            logger.debug("Updated date to: {}", tariffRateDto.getDate());
        }

        TariffRate savedRate = tariffRateRepository.save(existingRate);
        logger.debug("Saved rate: hsCode={}, importingCountryCode={}, exportingCountryCode={}, adValoremRate={}, date={}",
                    savedRate.getHsCode(),
                    savedRate.getImportingCountryCode(),
                    savedRate.getExportingCountryCode(),
                    savedRate.getAdValoremRate(),
                    savedRate.getDate());

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
        entity.setDate(dto.getDate());
        return entity;
    }
}
