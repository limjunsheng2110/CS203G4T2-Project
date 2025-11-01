package com.cs203.tariffg4t2.service.tariffLogic;

import com.cs203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.cs203.tariffg4t2.dto.response.TariffCalculationResultDTO;
import com.cs203.tariffg4t2.repository.basic.CountryProfileRepository;
import com.cs203.tariffg4t2.model.enums.ValuationBasis;
import com.cs203.tariffg4t2.repository.basic.AdditionalDutyMapRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the full tariff calculation flow:
 * 1) Build Customs Value (CIF or Transaction)
 * 2) Base Duty (MFN) using duty type (ad valorem / specific / compound / mixed)
 * 3) Apply FTA if RoO-eligible
 * 4) TRQ split (placeholder – wire to TariffRateDetail if available)
 * 5) Additional stacked duties (301/ADD/CVD/SG)
 * 6) VAT/GST on correct base
 * 7) Shipping and totals
 *
 * Assumptions:
 * - request.productValue is already in the destination currency.
 * - If your project converts currency elsewhere, keep productValue as-is.
 * - Percent legs from TariffRateService are scaled to use Customs Value, not raw invoice.
 */
@Service
public class TariffCalculatorService {

    @Autowired
    private TariffRateService tariffRateService;

    @Autowired
    private ShippingCostService shippingCostService;

    @Autowired
    private CountryProfileRepository countryProfileRepository;

    @Autowired
    private AdditionalDutyMapRepository additionalDutyMapRepository;

    @Autowired
    private TariffValidationService tariffValidationService;

    Logger logger = LoggerFactory.getLogger(TariffCalculatorService.class);


    // If you later create a CountryProfileService, inject it here to replace the simple helpers.
    @Transactional(readOnly = true) // <--- ADD THIS ANNOTATION
    public TariffCalculationResultDTO calculate(TariffCalculationRequestDTO request) {
        // Use the validation service - now sets defaults instead of just validating
        List<String> validationErrors = tariffValidationService.validateTariffRequest(request);

        // Log any missing or defaulted fields for monitoring
        if (!request.getMissingFields().isEmpty()) {
            logger.warn("Missing fields detected: {}", request.getMissingFields());
        }
        if (!request.getDefaultedFields().isEmpty()) {
            logger.info("Fields set to default values: {}", request.getDefaultedFields());
        }

        // Only fail if there are actual validation errors (not just missing fields that were defaulted)
        if (!validationErrors.isEmpty()) {
            String errorMessage = "Validation errors: " + String.join(", ", validationErrors);
            if (!request.getMissingFields().isEmpty()) {
                errorMessage += ". Missing fields (defaulted): " + String.join(", ", request.getMissingFields());
            }
            throw new IllegalArgumentException(errorMessage);
        }

        // ------------------------------------------------------------
        // 1) Valuation (Customs Value)
        // ------------------------------------------------------------
        var basis = resolveValuationBasis(request.getImportingCountry(), request.getValuationOverride());

        // By design choice here, productValue is already in destination currency.
        BigDecimal invoiceValueDest = safeBD(request.getProductValue());
        BigDecimal freight = safeBD(request.getFreight());
        BigDecimal insurance = safeBD(request.getInsurance());

        BigDecimal customsValue = invoiceValueDest;
        if (basis == ValuationBasis.CIF) {
            customsValue = customsValue.add(freight).add(insurance);
        }

        // ------------------------------------------------------------
        // 2) Base Duty (MFN) – computed by duty type; percent legs later scaled to Customs Value
        // ------------------------------------------------------------
        BigDecimal baseDuty = tariffRateService.calculateTariffAmount(request);

        if (invoiceValueDest.compareTo(BigDecimal.ZERO) > 0) {
            // Scale any percent components from productValue to customsValue
            BigDecimal percentScaler = customsValue.divide(invoiceValueDest, 10, RoundingMode.HALF_UP);
            baseDuty = baseDuty.multiply(percentScaler);
        }

        // ------------------------------------------------------------
        // 5) Additional stacked duties (e.g., Section 301 / ADD / CVD / Safeguard)
        //     For most jurisdictions these apply on Customs Value (confirm per country profile).
        // ------------------------------------------------------------

        java.time.LocalDate today = java.time.LocalDate.now();
        additionalDutyMapRepository
        .findFirstByImportingCountryAndExportingCountryAndHsCodeAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
                request.getImportingCountry(), request.getExportingCountry(), request.getHsCode(), today, today)
        .ifPresent(map -> {

                //20 % for us and china
                request.setSection301Rate(map.getSection301Rate());
                //exporting from really cheap country to well-to-do, add dumping rate
                request.setAntiDumpingRate(map.getAntiDumpingRate());
                //if the exporting country get subsidies from government
                request.setCountervailingRate(map.getCountervailingRate());
                request.setSafeguardRate(map.getSafeguardRate());
        });

        BigDecimal additional = BigDecimal.ZERO;
        if (isPositive(request.getSection301Rate())) {
            additional = additional.add(customsValue.multiply(request.getSection301Rate()));
        }
        if (isPositive(request.getAntiDumpingRate())) {
            additional = additional.add(customsValue.multiply(request.getAntiDumpingRate()));
        }
        if (isPositive(request.getCountervailingRate())) {
            additional = additional.add(customsValue.multiply(request.getCountervailingRate()));
        }
        if (isPositive(request.getSafeguardRate())) {
            additional = additional.add(customsValue.multiply(request.getSafeguardRate()));
        }

        // ------------------------------------------------------------
        // 6) VAT/GST (EU/SG style usually on CV + all duties; US typically none)
        // ------------------------------------------------------------
        BigDecimal vatRate = resolveVatRate(request.getImportingCountry(), request.getVatOrGstOverride());
        boolean vatIncludesDuties = resolveVatIncludesDuties(request.getImportingCountry());
        BigDecimal vatBase = vatIncludesDuties
                ? customsValue.add(baseDuty).add(additional)
                : customsValue;
        BigDecimal vatOrGst = vatRate.multiply(vatBase);

        // ------------------------------------------------------------
        // 7) Shipping & Totals
        // ------------------------------------------------------------
        BigDecimal shippingCost = shippingCostService.calculateShippingCost(request);

        BigDecimal tariffAmount = baseDuty.add(additional);

        BigDecimal totalCost = customsValue.add(tariffAmount).add(vatOrGst).add(shippingCost);

        // ------------------------------------------------------------
        // Build result DTO
        // ------------------------------------------------------------
        TariffCalculationResultDTO result = TariffCalculationResultDTO.builder()
                // Echo some inputs
                .importingCountry(request.getImportingCountry())
                .exportingCountry(request.getExportingCountry())
                .hsCode(request.getHsCode())
                .productValue(scale2(invoiceValueDest))
                .heads(request.getHeads() == null ? null : request.getHeads())
                .TariffType(null) // optionally fill from your TariffRate if you expose it here

                // Breakdown
                .customsValue(scale2(customsValue))
                .baseDuty(scale2(baseDuty))
                .additionalDuties(scale2(additional))
                .vatOrGst(scale2(vatOrGst))
                .shippingCost(scale2(shippingCost))

                // Totals
                .tariffAmount(scale2(tariffAmount))
                .totalCost(scale2(totalCost))

                // Meta
                .tradeAgreement(null) // fill if you track which FTA applied
                .calculationDate(LocalDateTime.now())

                // Optional rate echoes if your services expose them
                .adValoremRate(tariffRateService.getAdValoremRate(
                        request.getHsCode(),
                        request.getImportingCountry(),
                        request.getExportingCountry())
                )
                .build();

        // Add tracking information to result if needed
        if (!request.getMissingFields().isEmpty() || !request.getDefaultedFields().isEmpty()) {
            logger.info("Calculation completed with {} missing fields and {} defaulted fields",
                       request.getMissingFields().size(), request.getDefaultedFields().size());
        }

        return result;
    }

    // ------------------------------
    // Helpers
    // ------------------------------


    private BigDecimal safeBD(BigDecimal x) {
        return x == null ? BigDecimal.ZERO : x;
    }

    private BigDecimal scale2(BigDecimal x) {
        return x == null ? null : x.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isPositive(BigDecimal x) {
        return x != null && x.compareTo(BigDecimal.ZERO) > 0;
    }


    //removed to CountryProfileService later
    private ValuationBasis resolveValuationBasis(String importingCountry, String override) {
        if (override != null) {
            String up = override.trim().toUpperCase();
            if ("CIF".equals(up)) return ValuationBasis.CIF;
            if ("TRANSACTION".equals(up)) return ValuationBasis.TRANSACTION;
        }
        var cp = countryProfileRepository.findByCountryCode(importingCountry);
//        if (cp != null && cp.getValuationBasis() != null) return cp.getValuationBasis();
        return ValuationBasis.CIF;
    }

    private BigDecimal resolveVatRate(String importingCountry, BigDecimal override) {
        if (override != null) return override;
        var cp = countryProfileRepository.findByCountryCode(importingCountry);
        if (cp != null && cp.getVatOrGstRate() != null) return cp.getVatOrGstRate();
        return BigDecimal.ZERO;
    }

    private boolean resolveVatIncludesDuties(String importingCountry) {
        var cp = countryProfileRepository.findByCountryCode(importingCountry);
        return cp == null || cp.getVatIncludesDuties(); // default true
    }

}
