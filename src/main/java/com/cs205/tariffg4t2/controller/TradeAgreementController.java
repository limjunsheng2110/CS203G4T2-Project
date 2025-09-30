package com.cs205.tariffg4t2.controller;

import com.cs205.tariffg4t2.dto.request.TradeAgreementDto;
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
    public ResponseEntity<List<TradeAgreementDto>> getAllTradeAgreements() {
        List<TradeAgreementDto> agreements = tradeAgreementService.getAllTradeAgreements();
        return ResponseEntity.ok(agreements);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TradeAgreementDto> getTradeAgreementById(@PathVariable Long id) {
        TradeAgreementDto agreement = tradeAgreementService.getTradeAgreementById(id);
        return ResponseEntity.ok(agreement);
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<TradeAgreementDto> getTradeAgreementByName(@PathVariable String name) {
        TradeAgreementDto agreement = tradeAgreementService.getTradeAgreementByName(name);
        return ResponseEntity.ok(agreement);
    }

    @PostMapping
    public ResponseEntity<TradeAgreementDto> createTradeAgreement(@Valid @RequestBody TradeAgreementDto tradeAgreementDto) {
        TradeAgreementDto createdAgreement = tradeAgreementService.createTradeAgreement(tradeAgreementDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAgreement);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TradeAgreementDto> updateTradeAgreement(
            @PathVariable Long id,
            @Valid @RequestBody TradeAgreementDto tradeAgreementDto) {
        TradeAgreementDto updatedAgreement = tradeAgreementService.updateTradeAgreement(id, tradeAgreementDto);
        return ResponseEntity.ok(updatedAgreement);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTradeAgreement(@PathVariable Long id) {
        tradeAgreementService.deleteTradeAgreement(id);
        return ResponseEntity.noContent().build();
    }
}

