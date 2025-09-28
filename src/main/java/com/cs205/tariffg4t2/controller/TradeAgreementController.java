package com.cs205.tariffg4t2.controller;

import com.cs205.tariffg4t2.dto.request.TradeAgreementRequestDTO;
import com.cs205.tariffg4t2.dto.TradeAgreementDTO;
import com.cs205.tariffg4t2.service.basic.TradeAgreementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/trade-agreements")
@RequiredArgsConstructor
public class TradeAgreementController {

    private final TradeAgreementService tradeAgreementService;

    /**
     * GET /api/trade-agreements - Get all trade agreements
     */
    @GetMapping
    public ResponseEntity<List<TradeAgreementDTO>> getAllTradeAgreements() {
        try {
            List<TradeAgreementDTO> agreements = tradeAgreementService.getAllTradeAgreements();
            return ResponseEntity.ok(agreements);
        } catch (Exception e) {
            log.error("Error fetching all trade agreements: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/trade-agreements/{id} - Get trade agreement by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<TradeAgreementDTO> getTradeAgreementById(@PathVariable Long id) {
        try {
            return tradeAgreementService.getTradeAgreementById(id)
                    .map(agreement -> ResponseEntity.ok(agreement))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching trade agreement with id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/trade-agreements/name/{name} - Get trade agreement by name
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<TradeAgreementDTO> getTradeAgreementByName(@PathVariable String name) {
        try {
            return tradeAgreementService.getTradeAgreementByName(name)
                    .map(agreement -> ResponseEntity.ok(agreement))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching trade agreement with name {}: {}", name, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/trade-agreements/type/{type} - Get trade agreements by type
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<TradeAgreementDTO>> getTradeAgreementsByType(@PathVariable String type) {
        try {
            List<TradeAgreementDTO> agreements = tradeAgreementService.getTradeAgreementsByType(type);
            return ResponseEntity.ok(agreements);
        } catch (Exception e) {
            log.error("Error fetching trade agreements with type {}: {}", type, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/trade-agreements/active - Get active trade agreements
     */
    @GetMapping("/active")
    public ResponseEntity<List<TradeAgreementDTO>> getActiveTradeAgreements() {
        try {
            List<TradeAgreementDTO> activeAgreements = tradeAgreementService.getActiveTradeAgreements();
            return ResponseEntity.ok(activeAgreements);
        } catch (Exception e) {
            log.error("Error fetching active trade agreements: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/trade-agreements/country/{countryCode} - Get trade agreements by country
     */
    @GetMapping("/country/{countryCode}")
    public ResponseEntity<List<TradeAgreementDTO>> getTradeAgreementsByCountry(@PathVariable String countryCode) {
        try {
            List<TradeAgreementDTO> agreements = tradeAgreementService.getTradeAgreementsByCountry(countryCode);

            System.out.println("Agreements for country " + countryCode + ": " + agreements);
            return ResponseEntity.ok(agreements);
        } catch (Exception e) {
            log.error("Error fetching trade agreements for country {}: {}", countryCode, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/trade-agreements - Create new trade agreement
     */
    @PostMapping
    public ResponseEntity<?> createTradeAgreement(@Valid @RequestBody TradeAgreementRequestDTO request) {
        try {
            TradeAgreementDTO createdAgreement = tradeAgreementService.createTradeAgreement(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAgreement);
        } catch (RuntimeException e) {
            log.error("Error creating trade agreement: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error creating trade agreement: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating trade agreement: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred");
        }
    }

    /**
     * PUT /api/trade-agreements/{id} - Update trade agreement
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTradeAgreement(@PathVariable Long id,
                                                 @Valid @RequestBody TradeAgreementRequestDTO request) {
        try {
            TradeAgreementDTO updatedAgreement = tradeAgreementService.updateTradeAgreement(id, request);
            return ResponseEntity.ok(updatedAgreement);
        } catch (RuntimeException e) {
            log.error("Error updating trade agreement with id {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body("Error updating trade agreement: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error updating trade agreement with id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred");
        }
    }

    /**
     * DELETE /api/trade-agreements/{id} - Delete trade agreement
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTradeAgreement(@PathVariable Long id) {
        try {
            tradeAgreementService.deleteTradeAgreement(id);
            return ResponseEntity.ok("Trade agreement deleted successfully");
        } catch (RuntimeException e) {
            log.error("Error deleting trade agreement with id {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body("Error deleting trade agreement: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error deleting trade agreement with id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred");
        }
    }

    /**
     * POST /api/trade-agreements/{id}/members/{countryCode} - Add member country
     */
    @PostMapping("/{id}/members/{countryCode}")
    public ResponseEntity<?> addMemberCountry(@PathVariable Long id, @PathVariable String countryCode) {
        try {
            TradeAgreementDTO updatedAgreement = tradeAgreementService.addMemberCountry(id, countryCode);
            return ResponseEntity.ok(updatedAgreement);
        } catch (RuntimeException e) {
            log.error("Error adding country {} to agreement {}: {}", countryCode, id, e.getMessage());
            return ResponseEntity.badRequest().body("Error adding member country: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error adding country {} to agreement {}: {}", countryCode, id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred");
        }
    }

    /**
     * DELETE /api/trade-agreements/{id}/members/{countryCode} - Remove member country
     */
    @DeleteMapping("/{id}/members/{countryCode}")
    public ResponseEntity<?> removeMemberCountry(@PathVariable Long id, @PathVariable String countryCode) {
        try {
            TradeAgreementDTO updatedAgreement = tradeAgreementService.removeMemberCountry(id, countryCode);
            return ResponseEntity.ok(updatedAgreement);
        } catch (RuntimeException e) {
            log.error("Error removing country {} from agreement {}: {}", countryCode, id, e.getMessage());
            return ResponseEntity.badRequest().body("Error removing member country: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error removing country {} from agreement {}: {}", countryCode, id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred");
        }
    }
}
