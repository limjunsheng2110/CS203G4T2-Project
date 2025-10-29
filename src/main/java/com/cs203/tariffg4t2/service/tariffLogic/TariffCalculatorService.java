package com.cs203.tariffg4t2.service.tariffLogic;

import com.cs203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.cs203.tariffg4t2.dto.response.TariffCalculationResultDTO;
import com.cs203.tariffg4t2.model.basic.TariffRateDetail;
import com.cs203.tariffg4t2.repository.basic.CountryProfileRepository;
import com.cs203.tariffg4t2.repository.basic.TariffRateDetailRepository;
import com.cs203.tariffg4t2.model.enums.ValuationBasis;
import com.cs203.tariffg4t2.repository.basic.AdditionalDutyMapRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

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
    private TariffFTAService tariffFTAService;

    @Autowired
    private ShippingCostService shippingCostService;

    @Autowired
    private CountryProfileRepository countryProfileRepository;
    @Autowired
    private TariffRateDetailRepository tariffRateDetailRepository; // when you wire TRQ

    @Autowired
    private AdditionalDutyMapRepository additionalDutyMapRepository;


    // If you later create a CountryProfileService, inject it here to replace the simple helpers.

    public TariffCalculationResultDTO calculate(TariffCalculationRequestDTO request) {
        validateRequest(request);

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
        BigDecimal baseDutyProvisional = tariffRateService.calculateTariffAmount(request);

        BigDecimal baseDutyMFN = baseDutyProvisional;
        if (invoiceValueDest.compareTo(BigDecimal.ZERO) > 0) {
            // Scale any percent components from productValue to customsValue
            BigDecimal percentScaler = customsValue.divide(invoiceValueDest, 10, RoundingMode.HALF_UP);
            baseDutyMFN = baseDutyProvisional.multiply(percentScaler);
        }

        // ------------------------------------------------------------
        // 3) Apply FTA preference if RoO-eligible (service respects specific/compound/mixed)
        // ------------------------------------------------------------
        BigDecimal baseDuty = tariffFTAService.applyTradeAgreementDiscount(request, baseDutyMFN);
        if (baseDuty == null) {
            baseDuty = baseDutyMFN;
        }

        // ------------------------------------------------------------
        // 4) TRQ (Tariff Rate Quota) – placeholder. Wire to TariffRateDetail if available.
        // ------------------------------------------------------------
        BigDecimal trqDuty = BigDecimal.ZERO;
        Optional<TariffRateDetail> detailOpt = tariffRateDetailRepository.findFirstByTariffRate_HsCode(request.getHsCode());

        if (detailOpt.isPresent()) {
            TariffRateDetail detail = detailOpt.get();

            // Determine which unit applies for TRQ
            BigDecimal qty = BigDecimal.ZERO;
            String unit = detail.getUnitBasis(); // from DB

            if ("HEAD".equalsIgnoreCase(unit)) {
                // use heads if present
                if (request.getHeads() != null && request.getHeads() > 0) {
                    qty = new BigDecimal(request.getHeads());
                }
            } else if ("KG".equalsIgnoreCase(unit)) {
                // use weight if present
                if (request.getWeight() != null && request.getWeight().compareTo(BigDecimal.ZERO) > 0) {
                    qty = request.getWeight();
                }
            }

            // Only compute if we have a positive quantity and quota balance
            if (qty.compareTo(BigDecimal.ZERO) > 0 &&
                detail.getTrqQuotaBalance() != null &&
                detail.getTrqQuotaBalance().compareTo(BigDecimal.ZERO) > 0) {

                BigDecimal inQuota = qty.min(detail.getTrqQuotaBalance());
                BigDecimal outQuota = qty.subtract(inQuota).max(BigDecimal.ZERO);

                BigDecimal inQuotaDuty = BigDecimal.ZERO;
                BigDecimal outQuotaDuty = BigDecimal.ZERO;

                // In-quota leg
                if (isPositive(detail.getInQuotaRatePercent())) {
                    inQuotaDuty = inQuotaDuty.add(customsValue.multiply(detail.getInQuotaRatePercent()));
                }
                if (isPositive(detail.getInQuotaRateSpecific())) {
                    if ("HEAD".equalsIgnoreCase(unit)) {
                        inQuotaDuty = inQuotaDuty.add(
                            new BigDecimal(request.getHeads()).multiply(detail.getInQuotaRateSpecific()));
                    } else if ("KG".equalsIgnoreCase(unit)) {
                        inQuotaDuty = inQuotaDuty.add(
                            request.getWeight().multiply(detail.getInQuotaRateSpecific()));
                    }
                }

                // Out-of-quota leg
                if (isPositive(detail.getOutQuotaRatePercent())) {
                    outQuotaDuty = outQuotaDuty.add(customsValue.multiply(detail.getOutQuotaRatePercent()));
                }
                if (isPositive(detail.getOutQuotaRateSpecific())) {
                    if ("HEAD".equalsIgnoreCase(unit)) {
                        outQuotaDuty = outQuotaDuty.add(
                            new BigDecimal(request.getHeads()).multiply(detail.getOutQuotaRateSpecific()));
                    } else if ("KG".equalsIgnoreCase(unit)) {
                        outQuotaDuty = outQuotaDuty.add(
                            request.getWeight().multiply(detail.getOutQuotaRateSpecific()));
                    }
                }

                trqDuty = inQuotaDuty.add(outQuotaDuty);
            }
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
                request.setSection301Rate(map.getSection301Rate());
                request.setAntiDumpingRate(map.getAntiDumpingRate());
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
                ? customsValue.add(baseDuty).add(trqDuty).add(additional)
                : customsValue;
        BigDecimal vatOrGst = vatRate.multiply(vatBase);

        // ------------------------------------------------------------
        // 7) Shipping & Totals
        // ------------------------------------------------------------
        BigDecimal shippingCost = shippingCostService.calculateShippingCost(request);

        BigDecimal tariffAmount = trqDuty.intValue() == 0 ? baseDuty.add(additional) : trqDuty.add(additional);
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
                .trqDuty(scale2(trqDuty))
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
                .adValoremRate(null)
                .specificRate(null)
                .adValoremPreferentialRate(null)
                .specificPreferentialRate(null)
                .shippingRate(null)
                .build();

        return result;
    }

    // ------------------------------
    // Helpers
    // ------------------------------

    private void validateRequest(TariffCalculationRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null.");
        }
        if (request.getImportingCountry() == null || request.getImportingCountry().trim().isEmpty()) {
            throw new IllegalArgumentException("Importing country is required.");
        }
        if (request.getExportingCountry() == null || request.getExportingCountry().trim().isEmpty()) {
            throw new IllegalArgumentException("Exporting country is required.");
        }
        if (request.getHsCode() == null || request.getHsCode().trim().isEmpty()) {
            throw new IllegalArgumentException("HS code is required.");
        }
        if (request.getProductValue() == null) {
            throw new IllegalArgumentException("Product (invoice) value is required (destination currency).");
        }
        // Quantity sanity: at least one of heads or weight should be provided depending on the HS line used
        if ((request.getHeads() == null || request.getHeads() <= 0)
                && (request.getWeight() == null || request.getWeight().compareTo(BigDecimal.ZERO) <= 0)) {
            // We do not fail here because some ad valorem-only lines may not need units,
            // but your specific/compound/mixed logic will ask for the right unit as needed.
        }
    }

    private BigDecimal safeBD(BigDecimal x) {
        return x == null ? BigDecimal.ZERO : x;
    }

    private BigDecimal scale2(BigDecimal x) {
        return x == null ? null : x.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isPositive(BigDecimal x) {
        return x != null && x.compareTo(BigDecimal.ZERO) > 0;
    }

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
