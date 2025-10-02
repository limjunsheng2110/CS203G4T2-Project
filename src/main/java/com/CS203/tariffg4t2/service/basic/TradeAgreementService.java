package com.CS203.tariffg4t2.service.basic;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.CS203.tariffg4t2.dto.request.TradeAgreementDTO;
import com.CS203.tariffg4t2.model.basic.TradeAgreement;
import com.CS203.tariffg4t2.repository.basic.TradeAgreementRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TradeAgreementService {

    private final TradeAgreementRepository tradeAgreementRepository;

    @Transactional(readOnly = true)
    public List<TradeAgreementDTO> getAllTradeAgreements() {
        return tradeAgreementRepository.findAll()
                .stream()
                .map(this::convertEntityToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TradeAgreementDTO getTradeAgreementById(Long id) {
        TradeAgreement tradeAgreement = tradeAgreementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trade agreement not found with id: " + id));
        return convertEntityToDto(tradeAgreement);
    }

    @Transactional(readOnly = true)
    public TradeAgreementDTO getTradeAgreementByName(String name) {
        TradeAgreement tradeAgreement = tradeAgreementRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Trade agreement not found with name: " + name));
        return convertEntityToDto(tradeAgreement);
    }

    public TradeAgreementDTO createTradeAgreement(TradeAgreementDTO tradeAgreementDto) {
        validateTradeAgreementDto(tradeAgreementDto);

        // Check if name already exists
        if (tradeAgreementRepository.findByName(tradeAgreementDto.getName()).isPresent()) {
            throw new RuntimeException("Trade agreement with name already exists: " + tradeAgreementDto.getName());
        }

        TradeAgreement tradeAgreement = convertDtoToEntity(tradeAgreementDto);
        TradeAgreement savedAgreement = tradeAgreementRepository.save(tradeAgreement);
        return convertEntityToDto(savedAgreement);
    }

    public TradeAgreementDTO updateTradeAgreement(Long id, TradeAgreementDTO tradeAgreementDto) {
        validateTradeAgreementDto(tradeAgreementDto);

        TradeAgreement existingAgreement = tradeAgreementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trade agreement not found with id: " + id));

        // Check if name is being changed and already exists
        if (!existingAgreement.getName().equals(tradeAgreementDto.getName()) &&
            tradeAgreementRepository.findByName(tradeAgreementDto.getName()).isPresent()) {
            throw new RuntimeException("Trade agreement with name already exists: " + tradeAgreementDto.getName());
        }

        existingAgreement.setName(tradeAgreementDto.getName());
        existingAgreement.setEffectiveDate(tradeAgreementDto.getEffectiveDate());
        existingAgreement.setExpiryDate(tradeAgreementDto.getExpiryDate());

        TradeAgreement updatedAgreement = tradeAgreementRepository.save(existingAgreement);
        return convertEntityToDto(updatedAgreement);
    }

    public void deleteTradeAgreement(Long id) {
        if (!tradeAgreementRepository.existsById(id)) {
            throw new RuntimeException("Trade agreement not found with id: " + id);
        }
        tradeAgreementRepository.deleteById(id);
    }

    private TradeAgreementDTO convertEntityToDto(TradeAgreement entity) {
        TradeAgreementDTO dto = new TradeAgreementDTO();
        dto.setName(entity.getName());
        dto.setEffectiveDate(entity.getEffectiveDate());
        dto.setExpiryDate(entity.getExpiryDate());
        return dto;
    }

    private TradeAgreement convertDtoToEntity(TradeAgreementDTO dto) {
        TradeAgreement entity = new TradeAgreement();
        entity.setName(dto.getName());
        entity.setEffectiveDate(dto.getEffectiveDate());
        entity.setExpiryDate(dto.getExpiryDate());
        return entity;
    }

    private void validateTradeAgreementDto(TradeAgreementDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new RuntimeException("Trade agreement name cannot be null or empty");
        }
        if (dto.getEffectiveDate() == null) {
            throw new RuntimeException("Effective date cannot be null");
        }
        if (dto.getExpiryDate() == null) {
            throw new RuntimeException("Expiry date cannot be null");
        }
        if (dto.getExpiryDate().isBefore(dto.getEffectiveDate())) {
            throw new RuntimeException("Expiry date cannot be before effective date");
        }
    }
}

