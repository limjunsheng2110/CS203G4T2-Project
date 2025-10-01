package com.cs205.tariffg4t2.service.basic;

import com.cs205.tariffg4t2.dto.basic.PreferentialRateDTO;
import com.cs205.tariffg4t2.model.basic.PreferentialRate;
import com.cs205.tariffg4t2.model.basic.TradeAgreement;
import com.cs205.tariffg4t2.model.basic.Product;
import com.cs205.tariffg4t2.model.basic.Country;
import com.cs205.tariffg4t2.repository.basic.PreferentialRateRepository;
import com.cs205.tariffg4t2.repository.basic.TradeAgreementRepository;
import com.cs205.tariffg4t2.repository.basic.ProductRepository;
import com.cs205.tariffg4t2.repository.basic.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class PreferentialRateService {

    private final PreferentialRateRepository preferentialRateRepository;
    private final TradeAgreementRepository tradeAgreementRepository;
    private final ProductRepository productRepository;
    private final CountryRepository countryRepository;

    public PreferentialRateDTO createPreferentialRate(PreferentialRateDTO dto) {
        // Validate input DTO
        validatePreferentialRateDto(dto);

        // Fetch related entities
        TradeAgreement tradeAgreement = tradeAgreementRepository.findById(dto.getTradeAgreementId())
                .orElseThrow(() -> new RuntimeException("Trade agreement not found with id: " + dto.getTradeAgreementId()));

        Product product = productRepository.findById(dto.getHsCode())
                .orElseThrow(() -> new RuntimeException("Product not found with HS code: " + dto.getHsCode()));

        Country originCountry = countryRepository.findByCountryCode(dto.getOriginCountryId())
                .orElseThrow(() -> new RuntimeException("Origin country not found with country code: " + dto.getOriginCountryId()));

        Country destinationCountry = countryRepository.findByCountryCode(dto.getDestinationCountryId())
                .orElseThrow(() -> new RuntimeException("Destination country not found with country code: " + dto.getDestinationCountryId()));

        // Create entity with all references set
        PreferentialRate preferentialRate = new PreferentialRate();
        preferentialRate.setTradeAgreement(tradeAgreement);
        preferentialRate.setProduct(product);
        preferentialRate.setOriginCountry(originCountry);
        preferentialRate.setDestinationCountry(destinationCountry);
        preferentialRate.setPreferentialRate(dto.getPreferentialRate());

        PreferentialRate savedRate = preferentialRateRepository.save(preferentialRate);

        // Convert entity back to DTO
        PreferentialRateDTO result = new PreferentialRateDTO();
        result.setTradeAgreementId(savedRate.getTradeAgreement().getId());
        result.setHsCode(savedRate.getProduct().getHsCode());
        result.setOriginCountryId(savedRate.getOriginCountry().getCountryCode());
        result.setDestinationCountryId(savedRate.getDestinationCountry().getCountryCode());
        result.setPreferentialRate(savedRate.getPreferentialRate());

        return result;
    }

    // Basic validation logic


    private void validatePreferentialRateDto(PreferentialRateDTO dto) {
        if (dto.getTradeAgreementId() == null) {
            throw new RuntimeException("Trade agreement ID cannot be null");
        }
        if (dto.getHsCode() == null || dto.getHsCode().trim().isEmpty()) {
            throw new RuntimeException("HS code cannot be null or empty");
        }
        if (dto.getOriginCountryId() == null) {
            throw new RuntimeException("Origin country ID cannot be null");
        }
        if (dto.getDestinationCountryId() == null) {
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
    public BigDecimal getPreferentialRate(String originCountryCode, String destinationCountryCode, String hsCode) {
        PreferentialRate rate = preferentialRateRepository
                .findCustomPreferentialRate(originCountryCode, destinationCountryCode, hsCode)
                .orElse(null);

        return rate != null ? rate.getPreferentialRate() : null;
    }
}
