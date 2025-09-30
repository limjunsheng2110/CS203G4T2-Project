package com.cs205.tariffg4t2.service.basic;

import com.cs205.tariffg4t2.dto.TariffRateDTO;
import com.cs205.tariffg4t2.model.basic.TariffRate;
import com.cs205.tariffg4t2.repository.basic.TariffRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cs205.tariffg4t2.repository.basic.CountryRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class TariffRateService {

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
        return tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                hsCode, importingCountryCode, exportingCountryCode);
    }

    public TariffRate updateTariffRate(Long id, TariffRateDTO updatedTariffRateDto) {
        return tariffRateRepository.findById(id)
                .map(existingTariffRate -> {
                    existingTariffRate.setHsCode(updatedTariffRateDto.getHsCode());
                    existingTariffRate.setImportingCountryCode(updatedTariffRateDto.getImportingCountryCode());
                    existingTariffRate.setExportingCountryCode(updatedTariffRateDto.getExportingCountryCode());
                    existingTariffRate.setBaseRate(updatedTariffRateDto.getBaseRate());
                    existingTariffRate.setUnit(updatedTariffRateDto.getUnit());
                    existingTariffRate.setTariffType(updatedTariffRateDto.getTariffType());
                    existingTariffRate.setAdValoremRate(updatedTariffRateDto.getAdValoremRate());
                    existingTariffRate.setSpecificRateAmount(updatedTariffRateDto.getSpecificRateAmount());
                    return tariffRateRepository.save(existingTariffRate);
                })
                .orElseThrow(() -> new RuntimeException("TariffRate not found with id: " + id));
    }

    public void deleteTariffRate(Long id) {
        if (!tariffRateRepository.existsById(id)) {
            throw new RuntimeException("TariffRate not found with id: " + id);
        }
        tariffRateRepository.deleteById(id);
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

        if (dto.getAdValoremRate() != null && dto.getAdValoremRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Ad Valorem Rate cannot be negative");
        }

        if (dto.getSpecificRateAmount() != null && dto.getSpecificRateAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Specific Rate Amount cannot be negative");
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
        entity.setBaseRate(dto.getBaseRate());
        entity.setUnit(dto.getUnit() != null ? dto.getUnit().trim() : null);
        entity.setTariffType(dto.getTariffType() != null ? dto.getTariffType() : TariffRate.TariffType.AD_VALOREM);
        entity.setAdValoremRate(dto.getAdValoremRate());
        entity.setSpecificRateAmount(dto.getSpecificRateAmount());
        return entity;
    }
}
