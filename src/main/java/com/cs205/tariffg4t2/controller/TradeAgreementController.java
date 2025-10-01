package com.cs205.tariffg4t2.controller;

import com.cs205.tariffg4t2.dto.request.TradeAgreementDTO;
import com.cs205.tariffg4t2.service.basic.TradeAgreementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trade-agreements")
@RequiredArgsConstructor
public class TradeAgreementController {

    private final TradeAgreementService tradeAgreementService;

    @GetMapping
    public ResponseEntity<List<TradeAgreementDTO>> getAllTradeAgreements() {
        List<TradeAgreementDTO> agreements = tradeAgreementService.getAllTradeAgreements();
        return ResponseEntity.ok(agreements);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TradeAgreementDTO> getTradeAgreementById(@PathVariable Long id) {
        TradeAgreementDTO agreement = tradeAgreementService.getTradeAgreementById(id);
        return ResponseEntity.ok(agreement);
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<TradeAgreementDTO> getTradeAgreementByName(@PathVariable String name) {
        TradeAgreementDTO agreement = tradeAgreementService.getTradeAgreementByName(name);
        return ResponseEntity.ok(agreement);
    }

    @PostMapping
    public ResponseEntity<TradeAgreementDTO> createTradeAgreement(@Valid @RequestBody TradeAgreementDTO tradeAgreementDto) {
        TradeAgreementDTO createdAgreement = tradeAgreementService.createTradeAgreement(tradeAgreementDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAgreement);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TradeAgreementDTO> updateTradeAgreement(
            @PathVariable Long id,
            @Valid @RequestBody TradeAgreementDTO tradeAgreementDto) {
        TradeAgreementDTO updatedAgreement = tradeAgreementService.updateTradeAgreement(id, tradeAgreementDto);
        return ResponseEntity.ok(updatedAgreement);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTradeAgreement(@PathVariable Long id) {
        tradeAgreementService.deleteTradeAgreement(id);
        return ResponseEntity.noContent().build();
    }
}

