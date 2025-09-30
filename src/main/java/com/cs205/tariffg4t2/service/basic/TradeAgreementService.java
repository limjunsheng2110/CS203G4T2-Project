package com.cs205.tariffg4t2.service.basic;

import com.cs205.tariffg4t2.dto.request.TradeAgreementRequestDTO;
import com.cs205.tariffg4t2.dto.TradeAgreementDTO;
import com.cs205.tariffg4t2.model.basic.TradeAgreement;
import com.cs205.tariffg4t2.model.basic.Country;
import com.cs205.tariffg4t2.repository.basic.TradeAgreementRepository;
import com.cs205.tariffg4t2.repository.basic.CountryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TradeAgreementService {

    private final TradeAgreementRepository tradeAgreementRepository;
    private final CountryRepository countryRepository;

    /**
     * Get all trade agreements
     */
    @Transactional(readOnly = true)
    public List<TradeAgreementDTO> getAllTradeAgreements() {
        log.info("Fetching all trade agreements");
        return tradeAgreementRepository.findAll()
                .stream()
                .map(TradeAgreementDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Get trade agreement by ID
     */
    @Transactional(readOnly = true)
    public Optional<TradeAgreementDTO> getTradeAgreementById(Long id) {
        log.info("Fetching trade agreement with id: {}", id);
        return tradeAgreementRepository.findById(id)
                .map(TradeAgreementDTO::new);
    }

    /**
     * Get trade agreement by name
     */
    @Transactional(readOnly = true)
    public Optional<TradeAgreementDTO> getTradeAgreementByName(String name) {
        log.info("Fetching trade agreement with name: {}", name);
        return tradeAgreementRepository.findByName(name)
                .map(TradeAgreementDTO::new);
    }

    /**
     * Get trade agreements by type
     */
    @Transactional(readOnly = true)
    public List<TradeAgreementDTO> getTradeAgreementsByType(String type) {
        log.info("Fetching trade agreements with type: {}", type);
        return tradeAgreementRepository.findByType(type)
                .stream()
                .map(TradeAgreementDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Get currently active trade agreements
     */
    @Transactional(readOnly = true)
    public List<TradeAgreementDTO> getActiveTradeAgreements() {
        log.info("Fetching active trade agreements");
        return tradeAgreementRepository.findActiveAgreements(LocalDate.now())
                .stream()
                .map(TradeAgreementDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Get trade agreements by member country
     */
    @Transactional(readOnly = true)
    public List<TradeAgreementDTO> getTradeAgreementsByCountry(String countryCode) {
        log.info("Fetching trade agreements for country code: {}", countryCode);

        Country c = countryRepository.findByCountryCodeIgnoreCase(countryCode)
                .orElseThrow(() -> new RuntimeException("Country not found with code: " + countryCode));

        System.out.println("Country found: " + c.getCountryName());

        return tradeAgreementRepository.findByMemberCountry(countryCode)
            .stream()
            .map(TradeAgreementDTO::new)
            .collect(Collectors.toList());
    }

    /**
     * Create new trade agreement
     */
    public TradeAgreementDTO createTradeAgreement(TradeAgreementRequestDTO request) {
        log.info("Creating trade agreement with name: {}", request.getName());

        // Check if name already exists
        if (tradeAgreementRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("Trade agreement with name already exists: " + request.getName());
        }

        // Validate dates
        if (request.getExpiryDate() != null &&
            request.getExpiryDate().isBefore(request.getEffectiveDate())) {
            throw new RuntimeException("Expiry date cannot be before effective date");
        }

        TradeAgreement tradeAgreement = new TradeAgreement();
        tradeAgreement.setName(request.getName());
        tradeAgreement.setType(request.getType());
        tradeAgreement.setEffectiveDate(request.getEffectiveDate());
        tradeAgreement.setExpiryDate(request.getExpiryDate());

        // Set member countries
        if (request.getMemberCountryCodes() != null && !request.getMemberCountryCodes().isEmpty()) {
            Set<Country> memberCountries = new HashSet<>();
            for (String countryCode : request.getMemberCountryCodes()) {
                Country country = countryRepository.findByCountryCodeIgnoreCase(countryCode)
                        .orElseThrow(() -> new RuntimeException("Country not found with code: " + countryCode));
                memberCountries.add(country);
            }
            tradeAgreement.setMemberCountries(memberCountries);
        }

        TradeAgreement savedAgreement = tradeAgreementRepository.save(tradeAgreement);
        log.info("Trade agreement created successfully with id: {}", savedAgreement.getId());

        return new TradeAgreementDTO(savedAgreement);
    }

    /**
     * Update trade agreement
     */
    public TradeAgreementDTO updateTradeAgreement(Long id, TradeAgreementRequestDTO request) {
        log.info("Updating trade agreement with id: {}", id);

        TradeAgreement tradeAgreement = tradeAgreementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trade agreement not found with id: " + id));

        // Check if name is being changed and already exists
        if (!tradeAgreement.getName().equals(request.getName()) &&
            tradeAgreementRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("Trade agreement with name already exists: " + request.getName());
        }

        // Validate dates
        if (request.getExpiryDate() != null &&
            request.getExpiryDate().isBefore(request.getEffectiveDate())) {
            throw new RuntimeException("Expiry date cannot be before effective date");
        }

        tradeAgreement.setName(request.getName());
        tradeAgreement.setType(request.getType());
        tradeAgreement.setEffectiveDate(request.getEffectiveDate());
        tradeAgreement.setExpiryDate(request.getExpiryDate());

        // Update member countries
        if (request.getMemberCountryCodes() != null) {
            Set<Country> memberCountries = new HashSet<>();
            for (String countryCode : request.getMemberCountryCodes()) {
                Country country = countryRepository.findByCountryCodeIgnoreCase(countryCode)
                        .orElseThrow(() -> new RuntimeException("Country not found with code: " + countryCode));
                memberCountries.add(country);
            }
            tradeAgreement.setMemberCountries(memberCountries);
        }

        TradeAgreement updatedAgreement = tradeAgreementRepository.save(tradeAgreement);
        log.info("Trade agreement updated successfully with id: {}", updatedAgreement.getId());

        return new TradeAgreementDTO(updatedAgreement);
    }

    /**
     * Delete trade agreement
     */
    public void deleteTradeAgreement(Long id) {
        log.info("Deleting trade agreement with id: {}", id);

        if (!tradeAgreementRepository.existsById(id)) {
            throw new RuntimeException("Trade agreement not found with id: " + id);
        }

        tradeAgreementRepository.deleteById(id);
        log.info("Trade agreement deleted successfully with id: {}", id);
    }

    /**
     * Add member country to trade agreement
     */
    public TradeAgreementDTO addMemberCountry(Long agreementId, String countryCode) {
        log.info("Adding country {} to trade agreement {}", countryCode, agreementId);

        TradeAgreement tradeAgreement = tradeAgreementRepository.findById(agreementId)
                .orElseThrow(() -> new RuntimeException("Trade agreement not found with id: " + agreementId));

        Country country = countryRepository.findByCountryCodeIgnoreCase(countryCode)
                .orElseThrow(() -> new RuntimeException("Country not found with code: " + countryCode));

        if (tradeAgreement.getMemberCountries() == null) {
            tradeAgreement.setMemberCountries(new HashSet<>());
        }

        tradeAgreement.getMemberCountries().add(country);
        TradeAgreement updatedAgreement = tradeAgreementRepository.save(tradeAgreement);

        log.info("Country added successfully to trade agreement");
        return new TradeAgreementDTO(updatedAgreement);
    }

    /**
     * Remove member country from trade agreement
     */
    public TradeAgreementDTO removeMemberCountry(Long agreementId, String countryCode) {
        log.info("Removing country {} from trade agreement {}", countryCode, agreementId);

        TradeAgreement tradeAgreement = tradeAgreementRepository.findById(agreementId)
                .orElseThrow(() -> new RuntimeException("Trade agreement not found with id: " + agreementId));

        Country country = countryRepository.findByCountryCodeIgnoreCase(countryCode)
                .orElseThrow(() -> new RuntimeException("Country not found with code: " + countryCode));

        if (tradeAgreement.getMemberCountries() != null) {
            tradeAgreement.getMemberCountries().remove(country);
            TradeAgreement updatedAgreement = tradeAgreementRepository.save(tradeAgreement);
            log.info("Country removed successfully from trade agreement");
            return new TradeAgreementDTO(updatedAgreement);
        }

        return new TradeAgreementDTO(tradeAgreement);
    }
}
