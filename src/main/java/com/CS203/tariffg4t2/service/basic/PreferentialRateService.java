package com.CS203.tariffg4t2.service.basic;

import com.CS203.tariffg4t2.dto.basic.PreferentialRateDTO;
import com.CS203.tariffg4t2.model.basic.PreferentialRate;
import com.CS203.tariffg4t2.repository.basic.PreferentialRateRepository;
import com.CS203.tariffg4t2.repository.basic.TradeAgreementRepository;
import com.CS203.tariffg4t2.repository.basic.ProductRepository;
import com.CS203.tariffg4t2.repository.basic.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PreferentialRateService {

    private final PreferentialRateRepository preferentialRateRepository;
    private final TradeAgreementRepository tradeAgreementRepository;
    private final ProductRepository productRepository;
    private final CountryRepository countryRepository;

    // Basic validation logic


    private void validatePreferentialRateDto(PreferentialRateDTO dto) {
        if (dto.getTradeAgreementId() == null) {
            throw new RuntimeException("Trade agreement ID cannot be null");
        }
        if (dto.getHsCode() == null || dto.getHsCode().trim().isEmpty()) {
            throw new RuntimeException("HS code cannot be null or empty");
        }
        if (dto.getExportingCountryCode() == null) {
            throw new RuntimeException("Origin country ID cannot be null");
        }
        if (dto.getImportingCountryCode() == null) {
            throw new RuntimeException("Destination country ID cannot be null");
        }
        if (dto.getPreferentialRate() == null) {
            throw new RuntimeException("Preferential rate cannot be null");
        }
        if (dto.getPreferentialRate().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Preferential rate cannot be negative");
        }
    }


    // Fetch preferential rate based on origin, destination, and HS code (USED IN TARIFF LOGIC)
    public BigDecimal getPreferentialRate(String importingCountryCode, String exportingCountryCode, String hsCode) {
        PreferentialRate rate = preferentialRateRepository
                .findCustomPreferentialRate(importingCountryCode, exportingCountryCode, hsCode)
                .orElse(null);

        return rate != null ? rate.getPreferentialRate() : null;
    }

    public Optional<PreferentialRate> findPreferentialRate(String importingCountryCode, String exportingCountryCode, String hsCode) {
        return preferentialRateRepository.findCustomPreferentialRate(importingCountryCode, exportingCountryCode, hsCode);
    }
}
