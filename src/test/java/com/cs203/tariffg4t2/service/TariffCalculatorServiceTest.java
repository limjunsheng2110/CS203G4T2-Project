package com.cs203.tariffg4t2.service;

import com.cs203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.cs203.tariffg4t2.dto.response.TariffCalculationResultDTO;
import com.cs203.tariffg4t2.model.basic.TariffRate;
import com.cs203.tariffg4t2.model.basic.Country;
import com.cs203.tariffg4t2.repository.basic.CountryRepository;
import com.cs203.tariffg4t2.service.tariffLogic.ShippingCostService;
import com.cs203.tariffg4t2.service.tariffLogic.TariffCalculatorService;
import com.cs203.tariffg4t2.service.tariffLogic.TariffRateService;
import com.cs203.tariffg4t2.service.tariffLogic.TariffValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TariffCalculatorServiceTest {

    @Mock
    private TariffRateService tariffRateService;

    @Mock
    private ShippingCostService shippingCostService;

    @Mock
    private TariffValidationService tariffValidationService;

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private TariffCalculatorService tariffCalculatorService;

    private TariffCalculationRequestDTO validRequest;
    private TariffRate mockTariffRate;
    private Country mockCountry;

    @BeforeEach
    void setUp() {
        // Setup valid request
        validRequest = new TariffCalculationRequestDTO();
        validRequest.setImportingCountry("SG");
        validRequest.setExportingCountry("US");
        validRequest.setHsCode("010329");
        validRequest.setProductValue(new BigDecimal("10000.00"));
        validRequest.setFreight(new BigDecimal("200.00"));
        validRequest.setInsurance(new BigDecimal("50.00"));
        validRequest.setHeads(100);
        validRequest.setWeight(new BigDecimal("50.00"));

        // Setup mock tariff rate
        mockTariffRate = new TariffRate();
        mockTariffRate.setHsCode("010329");
        mockTariffRate.setImportingCountryCode("SG");
        mockTariffRate.setExportingCountryCode("US");
        mockTariffRate.setYear(2024);

        // Setup mock country with VAT rate
        mockCountry = new Country();
        mockCountry.setCountryCode("SG");
        mockCountry.setCountryName("Singapore");
        mockCountry.setVatRate(new BigDecimal("9")); // 9% GST (stored as whole number)
    }

    // basic calculation tests

    @Test
    void testCalculate_Success_CIFValuation() {
        // given
        when(tariffValidationService.validateTariffRequest(any())).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(any())).thenReturn(new BigDecimal("512.50"));
        when(tariffRateService.getTariffRate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(mockTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("0.05"));
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("200.00"));
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(mockCountry));

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        assertNotNull(result);
        assertEquals(new BigDecimal("10250.00"), result.getCustomsValue()); // 10000 + 200 + 50
        assertEquals(new BigDecimal("525.31"), result.getBaseDuty());
        assertEquals(new BigDecimal("200.00"), result.getShippingCost());
        assertEquals(Integer.valueOf(2024), result.getYear());
        assertNotNull(result.getCalculationDate());
    }

    @Test
    void testCalculate_Success_WithTransactionValue() {
        // given
        when(tariffValidationService.validateTariffRequest(any())).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(any())).thenReturn(new BigDecimal("500.00"));
        when(tariffRateService.getTariffRate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(mockTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("0.05"));
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("200.00"));
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.empty());

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        // CIF valuation includes freight and insurance
        assertEquals(new BigDecimal("10250.00"), result.getCustomsValue());
    }

    // ========== VAT/GST TESTS ==========

    @Test
    void testCalculate_VatIncludesDuties() {
        // given
        validRequest.setVatOrGstOverride(new BigDecimal("0.09")); // 9% GST

        when(tariffValidationService.validateTariffRequest(any())).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(any())).thenReturn(new BigDecimal("512.50"));
        when(tariffRateService.getTariffRate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(mockTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("0.05"));
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("200.00"));

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        // VAT base = 10250 + 525.31 = 10775.31
        // VAT = 10775.31 * 0.09 = 969.78 (rounded)
        assertEquals(new BigDecimal("969.78"), result.getVatOrGst());
    }

    @Test
    void testCalculate_NoVatWhenNotProvided() {
        // given
        when(tariffValidationService.validateTariffRequest(any())).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(any())).thenReturn(new BigDecimal("512.50"));
        when(tariffRateService.getTariffRate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(mockTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("0.05"));
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("200.00"));
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.empty());

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        // Should default to 0 VAT when country not found
        assertEquals(new BigDecimal("0.00"), result.getVatOrGst());
    }

    @Test
    void testCalculate_WithVatOverride() {
        // given
        validRequest.setVatOrGstOverride(new BigDecimal("0.20")); // 20% VAT override

        when(tariffValidationService.validateTariffRequest(any())).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(any())).thenReturn(new BigDecimal("512.50"));
        when(tariffRateService.getTariffRate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(mockTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("0.05"));
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("200.00"));

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        // Should use override rate of 20%
        assertNotNull(result.getVatOrGst());
        assertTrue(result.getVatOrGst().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testCalculate_VatFromCountryDatabase() {
        // given
        when(tariffValidationService.validateTariffRequest(any())).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(any())).thenReturn(new BigDecimal("512.50"));
        when(tariffRateService.getTariffRate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(mockTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("0.05"));
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("200.00"));
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(mockCountry));

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        // Should use VAT rate from country (9%)
        // VAT base = 10250 + 525.31 = 10775.31
        // VAT = 10775.31 * 0.09 = 969.78
        assertNotNull(result.getVatOrGst());
        assertEquals(new BigDecimal("969.78"), result.getVatOrGst());
    }

    // validation tests

    @Test
    void testCalculate_ValidationErrors() {
        // given
        ArrayList<String> errors = new ArrayList<>();
        errors.add("Invalid HS code");
        when(tariffValidationService.validateTariffRequest(any())).thenReturn(errors);

        // when & then
        assertThrows(IllegalArgumentException.class, () ->
            tariffCalculatorService.calculate(validRequest)
        );
    }

    @Test
    void testCalculate_WithMissingFieldsButDefaulted() {
        // given
        validRequest.setFreight(null);
        validRequest.setInsurance(null);

        when(tariffValidationService.validateTariffRequest(any())).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(any())).thenReturn(new BigDecimal("500.00"));
        when(tariffRateService.getTariffRate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(mockTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("0.05"));
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("200.00"));
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.empty());

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        assertNotNull(result);
        // Customs value should be just product value when freight/insurance are null (default to 0)
        assertEquals(new BigDecimal("10000.00"), result.getCustomsValue());
    }

    // edge case tests

    @Test
    void testCalculate_ZeroProductValue() {
        // given
        validRequest.setProductValue(BigDecimal.ZERO);

        when(tariffValidationService.validateTariffRequest(any())).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(any())).thenReturn(BigDecimal.ZERO);
        when(tariffRateService.getTariffRate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(mockTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(BigDecimal.ZERO);
        when(shippingCostService.calculateShippingCost(any())).thenReturn(BigDecimal.ZERO);
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.empty());

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        assertNotNull(result);
        assertTrue(result.getTotalCost().compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    void testCalculate_VeryLargeProductValue() {
        // given
        validRequest.setProductValue(new BigDecimal("999999999.99"));

        when(tariffValidationService.validateTariffRequest(any())).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(any())).thenReturn(new BigDecimal("50000000.00"));
        when(tariffRateService.getTariffRate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(mockTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("0.05"));
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("5000.00"));
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.empty());

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        assertNotNull(result);
        assertTrue(result.getTotalCost().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testCalculate_NoTariffRateDate() {
        // given
        when(tariffValidationService.validateTariffRequest(any())).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(any())).thenReturn(new BigDecimal("512.50"));
        when(tariffRateService.getTariffRate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty()); // No tariff rate found
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("0.05"));
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("200.00"));
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.empty());

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        assertNotNull(result);
        assertNull(result.getYear()); // Year should be null when no tariff rate found
    }

    // total cost verification tests

    @Test
    void testCalculate_TotalCostCalculation() {
        // given
        when(tariffValidationService.validateTariffRequest(any())).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(any())).thenReturn(new BigDecimal("512.50"));
        when(tariffRateService.getTariffRate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(mockTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("0.05"));
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("200.00"));
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(mockCountry));

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        // Total = CustomsValue + TariffAmount + VAT + Shipping
        BigDecimal expectedTotal = result.getCustomsValue()
                .add(result.getTariffAmount())
                .add(result.getVatOrGst())
                .add(result.getShippingCost());
        
        assertEquals(expectedTotal, result.getTotalCost());
    }
}
