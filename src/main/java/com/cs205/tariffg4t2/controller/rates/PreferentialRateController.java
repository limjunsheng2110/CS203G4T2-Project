package com.cs205.tariffg4t2.controller.rates;

import com.cs205.tariffg4t2.dto.basic.PreferentialRateDTO;
import com.cs205.tariffg4t2.service.basic.PreferentialRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/preferential-rates")
@RequiredArgsConstructor
public class PreferentialRateController {

    private final PreferentialRateService preferentialRateService;

    @PostMapping
    public ResponseEntity<PreferentialRateDTO> createPreferentialRate(@Valid @RequestBody PreferentialRateDTO preferentialRateDto) {
        PreferentialRateDTO createdRate = preferentialRateService.createPreferentialRate(preferentialRateDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRate);
    }
}

