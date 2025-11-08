package com.cs203.tariffg4t2.service.tariffLogic;

import com.cs203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.cs203.tariffg4t2.dto.response.TariffCalculationResultDTO;
import com.cs203.tariffg4t2.model.basic.TariffRate;
import com.cs203.tariffg4t2.model.basic.Country;
import com.cs203.tariffg4t2.repository.basic.CountryRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestrates the full tariff calculation flow:
 * 1) Build Customs Value (CIF or Transaction)
 * 2) Base Duty (MFN) using duty type (ad valorem / specific / compound / mixed)
 * 3) Apply FTA if RoO-eligible
 * 4) TRQ split (placeholder – wire to TariffRateDetail if available)
 * 5) VAT/GST on correct base (from Country VAT rate)
 * 6) Shipping and totals
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
    private TariffValidationService tariffValidationService;

    @Autowired
    private CountryRepository countryRepository;

    Logger logger = LoggerFactory.getLogger(TariffCalculatorService.class);

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
        // 1) Valuation (Customs Value) - Default to CIF
        // ------------------------------------------------------------
        BigDecimal invoiceValueDest = safeBD(request.getProductValue());
        BigDecimal freight = safeBD(request.getFreight());
        BigDecimal insurance = safeBD(request.getInsurance());

        // Default to CIF valuation (includes freight and insurance)
        BigDecimal customsValue = invoiceValueDest.add(freight).add(insurance);

        // ------------------------------------------------------------
        // 2) Base Duty (MFN) – computed by duty type; percent legs later scaled to Customs Value
        // ------------------------------------------------------------
        BigDecimal baseDuty = tariffRateService.calculateTariffAmount(request);

        // Get the actual tariff rate that was used (with year information)
        Optional<TariffRate> usedTariffRate = tariffRateService.getTariffRateWithYear(
            request.getHsCode(),
            request.getImportingCountry(),
            request.getExportingCountry(),
            request.getYear()
        );
        Integer actualYear = usedTariffRate.map(TariffRate::getYear).orElse(null);

        if (invoiceValueDest.compareTo(BigDecimal.ZERO) > 0) {
            // Scale any percent components from productValue to customsValue
            BigDecimal percentScaler = customsValue.divide(invoiceValueDest, 10, RoundingMode.HALF_UP);
            baseDuty = baseDuty.multiply(percentScaler);
        }

        // ------------------------------------------------------------
        // 3) VAT/GST - Fetch from Country table for importing country
        // ------------------------------------------------------------
        BigDecimal vatRatePercentage = BigDecimal.ZERO;

        // Use override if provided (for testing), otherwise fetch from Country table
        if (request.getVatOrGstOverride() != null) {
            vatRatePercentage = request.getVatOrGstOverride();
            logger.debug("Using VAT override: {}", vatRatePercentage);
        } else {
            Optional<Country> importingCountry = countryRepository.findByCountryCodeIgnoreCase(request.getImportingCountry());
            if (importingCountry.isPresent() && importingCountry.get().getVatRate() != null) {
                vatRatePercentage = importingCountry.get().getVatRate();
                logger.debug("Using VAT rate from Country {}: {}%", request.getImportingCountry(), vatRatePercentage);
            } else {
                logger.debug("No VAT rate found for country {}, defaulting to 0", request.getImportingCountry());
            }
        }

        // Convert percentage to decimal (e.g., 10% -> 0.10)
        BigDecimal vatRate = vatRatePercentage.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);

        // VAT is applied on (customsValue + baseDuty)
        BigDecimal vatBase = customsValue.add(baseDuty);
        BigDecimal vatOrGst = vatRate.multiply(vatBase).setScale(2, RoundingMode.HALF_UP);

        // ------------------------------------------------------------
        // 4) Shipping & Totals
        // ------------------------------------------------------------
        BigDecimal shippingCost = shippingCostService.calculateShippingCost(request);

        BigDecimal tariffAmount = baseDuty;

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
                .vatOrGst(scale2(vatOrGst))
                .shippingCost(scale2(shippingCost))

                // Totals
                .tariffAmount(scale2(tariffAmount))
                .totalCost(scale2(totalCost))

                // Meta
                .tradeAgreement(null) // fill if you track which FTA applied
                .calculationDate(LocalDateTime.now())
                .year(actualYear) // Use the actual year from the tariff rate that was found

                // Optional rate echoes if your services expose them
                .adValoremRate(tariffRateService.getAdValoremRate(
                        request.getHsCode(),
                        request.getImportingCountry(),
                        request.getExportingCountry())
                )
                .vatRate(vatRatePercentage)  // Store as percentage (10 for 10%), not decimal (0.10)
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
}
