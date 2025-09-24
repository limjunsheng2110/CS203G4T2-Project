package com.cs205.tariffg4t2.controller;

import com.cs205.tariffg4t2.dto.request.TariffCalculationRequest;
import com.cs205.tariffg4t2.dto.response.TariffCalculationResponse;
import com.cs205.tariffg4t2.dto.response.TariffCalculationResult;
import com.cs205.tariffg4t2.model.api.ScrapingJob;
import com.cs205.tariffg4t2.service.tariffLogic.TariffCalculatorService;
import com.cs205.tariffg4t2.service.WebScrapingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tariff")
public class TariffController {

    @Autowired
    private TariffCalculatorService tariffService;

    @Autowired
    private WebScrapingService webScrapingService;

    @GetMapping("/calculate")
    public ResponseEntity<TariffCalculationResponse> calculateTariff(
            @RequestParam String homeCountry,
            @RequestParam String destinationCountry,
            @RequestParam String productName,
            @RequestParam BigDecimal productValue,
            @RequestParam(required = false) String hsCode,
            @RequestParam(required = false) String tradeAgreement) {

        try {
            // Create calculation request
            TariffCalculationRequest request = TariffCalculationRequest.builder()
                    .homeCountry(homeCountry.trim().toUpperCase())
                    .destinationCountry(destinationCountry.trim().toUpperCase())
                    .productName(productName.trim())
                    .productValue(productValue)
                    .hsCode(hsCode)
                    .tradeAgreement(tradeAgreement)
                    .build();

            // Calculate tariff (validation is handled in the service layer)
            TariffCalculationResult result = tariffService.calculateTariff(request);

            return ResponseEntity.ok(new TariffCalculationResponse("Success", result));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new TariffCalculationResponse(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new TariffCalculationResponse("Internal server error", null));
        }
    }


    /**
     * Test scraping the specific UK tariff URL
     */
    @PostMapping("/test-uk-scraping")
    public ResponseEntity<Map<String, Object>> testUKScraping() {
        try {
            String testUrl = "https://www.trade-tariff.service.gov.uk/subheadings/0201100000-80?day=24&month=9&year=2025";

            ScrapingJob job = webScrapingService.scrapeUKTariffUrl(testUrl);

            Map<String, Object> response = new HashMap<>();
            response.put("status", job.getStatus());
            response.put("recordsExtracted", job.getRecordsExtracted());
            response.put("jobId", job.getId());
            response.put("errorMessage", job.getErrorMessage());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}