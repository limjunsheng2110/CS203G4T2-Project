package com.cs203.tariffg4t2.service;

import com.cs203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.cs203.tariffg4t2.dto.response.TariffCalculationResultDTO;
import com.cs203.tariffg4t2.model.basic.AdditionalDutyMap;
import com.cs203.tariffg4t2.model.basic.CountryProfile;
import com.cs203.tariffg4t2.model.basic.TariffRate;
import com.cs203.tariffg4t2.repository.basic.AdditionalDutyMapRepository;
import com.cs203.tariffg4t2.repository.basic.CountryProfileRepository;
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
import java.time.LocalDate;
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
    private CountryProfileRepository countryProfileRepository;

    @Mock
    private AdditionalDutyMapRepository additionalDutyMapRepository;

    @Mock
    private TariffValidationService tariffValidationService;

    @InjectMocks
    private TariffCalculatorService tariffCalculatorService;

    private TariffCalculationRequestDTO validRequest;
    private CountryProfile sgProfile;
    private TariffRate mockTariffRate;

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

        // Setup SG country profile
        sgProfile = new CountryProfile();
        sgProfile.setCountryCode("SG");
        sgProfile.setVatOrGstRate(new BigDecimal("0.09")); // 9% GST
        sgProfile.setVatIncludesDuties(true);

        // Setup mock tariff rate
        mockTariffRate = new TariffRate();
        mockTariffRate.setHsCode("010329");
        mockTariffRate.setImportingCountryCode("SG");
        mockTariffRate.setExportingCountryCode("US");
        mockTariffRate.setDate("2024-11-01");
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
        when(countryProfileRepository.findByCountryCode("SG")).thenReturn(sgProfile);
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("200.00"));
        when(additionalDutyMapRepository
                .findFirstByImportingCountryAndExportingCountryAndHsCodeAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
                        anyString(), anyString(), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        assertNotNull(result);
        assertEquals(new BigDecimal("10250.00"), result.getCustomsValue()); // 10000 + 200 + 50
        assertEquals(new BigDecimal("525.31"), result.getBaseDuty());
        assertEquals(new BigDecimal("0.00"), result.getAdditionalDuties());
        assertEquals(new BigDecimal("200.00"), result.getShippingCost());
        assertEquals("2024-11-01", result.getDate());
        assertNotNull(result.getCalculationDate());
    }

    @Test
    void testCalculate_Success_TransactionValuation() {
        // given
        validRequest.setValuationOverride("TRANSACTION");
        when(tariffValidationService.validateTariffRequest(any())).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(any())).thenReturn(new BigDecimal("500.00"));
        when(tariffRateService.getTariffRate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(mockTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("0.05"));
        when(countryProfileRepository.findByCountryCode("SG")).thenReturn(sgProfile);
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("200.00"));
        when(additionalDutyMapRepository
                .findFirstByImportingCountryAndExportingCountryAndHsCodeAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
                        anyString(), anyString(), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        assertEquals(new BigDecimal("10000.00"), result.getCustomsValue()); // Transaction value only
    }

    // additional duties tests

    @Test
    void testCalculate_WithSection301Duty() {
        // given
        AdditionalDutyMap dutyMap = new AdditionalDutyMap();
        dutyMap.setSection301Rate(new BigDecimal("0.25")); // 25% Section 301
        dutyMap.setAntiDumpingRate(BigDecimal.ZERO);
        dutyMap.setCountervailingRate(BigDecimal.ZERO);
        dutyMap.setSafeguardRate(BigDecimal.ZERO);

        when(tariffValidationService.validateTariffRequest(any())).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(any())).thenReturn(new BigDecimal("512.50"));
        when(tariffRateService.getTariffRate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(mockTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("0.05"));
        when(countryProfileRepository.findByCountryCode("SG")).thenReturn(sgProfile);
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("200.00"));
        when(additionalDutyMapRepository
                .findFirstByImportingCountryAndExportingCountryAndHsCodeAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
                        anyString(), anyString(), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.of(dutyMap));

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        // Section 301: 10250 * 0.25 = 2562.50
        assertEquals(new BigDecimal("2562.50"), result.getAdditionalDuties());
        verify(additionalDutyMapRepository, times(1))
                .findFirstByImportingCountryAndExportingCountryAndHsCodeAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
                        anyString(), anyString(), anyString(), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testCalculate_WithAntiDumpingDuty() {
        // given
        AdditionalDutyMap dutyMap = new AdditionalDutyMap();
        dutyMap.setSection301Rate(BigDecimal.ZERO);
        dutyMap.setAntiDumpingRate(new BigDecimal("0.15")); // 15% Anti-Dumping
        dutyMap.setCountervailingRate(BigDecimal.ZERO);
        dutyMap.setSafeguardRate(BigDecimal.ZERO);

        when(tariffValidationService.validateTariffRequest(any())).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(any())).thenReturn(new BigDecimal("512.50"));
        when(tariffRateService.getTariffRate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(mockTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("0.05"));
        when(countryProfileRepository.findByCountryCode("SG")).thenReturn(sgProfile);
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("200.00"));
        when(additionalDutyMapRepository
                .findFirstByImportingCountryAndExportingCountryAndHsCodeAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
                        anyString(), anyString(), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.of(dutyMap));

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        // Anti-Dumping: 10250 * 0.15 = 1537.50
        assertEquals(new BigDecimal("1537.50"), result.getAdditionalDuties());
    }

    @Test
    void testCalculate_WithMultipleAdditionalDuties() {
        // given
        AdditionalDutyMap dutyMap = new AdditionalDutyMap();
        dutyMap.setSection301Rate(new BigDecimal("0.25")); // 25%
        dutyMap.setAntiDumpingRate(new BigDecimal("0.15")); // 15%
        dutyMap.setCountervailingRate(new BigDecimal("0.10")); // 10%
        dutyMap.setSafeguardRate(new BigDecimal("0.05")); // 5%

        when(tariffValidationService.validateTariffRequest(any())).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(any())).thenReturn(new BigDecimal("512.50"));
        when(tariffRateService.getTariffRate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(mockTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("0.05"));
        when(countryProfileRepository.findByCountryCode("SG")).thenReturn(sgProfile);
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("200.00"));
        when(additionalDutyMapRepository
                .findFirstByImportingCountryAndExportingCountryAndHsCodeAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
                        anyString(), anyString(), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.of(dutyMap));

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        // Total additional: 10250 * (0.25 + 0.15 + 0.10 + 0.05) = 10250 * 0.55 = 5637.50
        assertEquals(new BigDecimal("5637.50"), result.getAdditionalDuties());
    }

    // ========== VAT/GST TESTS ==========

    @Test
    void testCalculate_VatIncludesDuties() {
        // given
        sgProfile.setVatIncludesDuties(true);
        sgProfile.setVatOrGstRate(new BigDecimal("0.09")); // 9% GST

        when(tariffValidationService.validateTariffRequest(any())).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(any())).thenReturn(new BigDecimal("512.50"));
        when(tariffRateService.getTariffRate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(mockTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("0.05"));
        when(countryProfileRepository.findByCountryCode("SG")).thenReturn(sgProfile);
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("200.00"));
        when(additionalDutyMapRepository
                .findFirstByImportingCountryAndExportingCountryAndHsCodeAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
                        anyString(), anyString(), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        // VAT base = 10250 + 525.31 = 10775.31
        // VAT = 10775.31 * 0.09 = 969.78 (rounded)
        assertEquals(new BigDecimal("969.78"), result.getVatOrGst());
    }

    @Test
    void testCalculate_VatExcludesDuties() {
        // given
        sgProfile.setVatIncludesDuties(false);
        sgProfile.setVatOrGstRate(new BigDecimal("0.09"));

        when(tariffValidationService.validateTariffRequest(any())).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(any())).thenReturn(new BigDecimal("512.50"));
        when(tariffRateService.getTariffRate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(mockTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("0.05"));
        when(countryProfileRepository.findByCountryCode("SG")).thenReturn(sgProfile);
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("200.00"));
        when(additionalDutyMapRepository
                .findFirstByImportingCountryAndExportingCountryAndHsCodeAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
                        anyString(), anyString(), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        // VAT base = 10250 (customs value only)
        // VAT = 10250 * 0.09 = 922.50
        assertEquals(new BigDecimal("922.50"), result.getVatOrGst());
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
        when(countryProfileRepository.findByCountryCode("SG")).thenReturn(sgProfile);
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("200.00"));
        when(additionalDutyMapRepository
                .findFirstByImportingCountryAndExportingCountryAndHsCodeAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
                        anyString(), anyString(), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        // Should use override rate of 20% instead of profile's 9%
        assertNotNull(result.getVatOrGst());
        assertTrue(result.getVatOrGst().compareTo(BigDecimal.ZERO) > 0);
    }

    // validation tests

    @Test
    void testCalculate_ValidationErrors() {
        // given
        ArrayList<String> errors = new ArrayList<>();
        errors.add("Invalid HS code");
        when(tariffValidationService.validateTariffRequest(any())).thenReturn(errors);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            tariffCalculatorService.calculate(validRequest);
        });
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
        when(countryProfileRepository.findByCountryCode("SG")).thenReturn(sgProfile);
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("200.00"));
        when(additionalDutyMapRepository
                .findFirstByImportingCountryAndExportingCountryAndHsCodeAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
                        anyString(), anyString(), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.empty());

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
        when(countryProfileRepository.findByCountryCode("SG")).thenReturn(sgProfile);
        when(shippingCostService.calculateShippingCost(any())).thenReturn(BigDecimal.ZERO);
        when(additionalDutyMapRepository
                .findFirstByImportingCountryAndExportingCountryAndHsCodeAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
                        anyString(), anyString(), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.empty());

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
        when(countryProfileRepository.findByCountryCode("SG")).thenReturn(sgProfile);
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("5000.00"));
        when(additionalDutyMapRepository
                .findFirstByImportingCountryAndExportingCountryAndHsCodeAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
                        anyString(), anyString(), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        assertNotNull(result);
        assertTrue(result.getTotalCost().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testCalculate_NoCountryProfile() {
        // given
        when(tariffValidationService.validateTariffRequest(any())).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(any())).thenReturn(new BigDecimal("512.50"));
        when(tariffRateService.getTariffRate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(mockTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("0.05"));
        when(countryProfileRepository.findByCountryCode("SG")).thenReturn(null); // No profile
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("200.00"));
        when(additionalDutyMapRepository
                .findFirstByImportingCountryAndExportingCountryAndHsCodeAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
                        anyString(), anyString(), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        assertNotNull(result);
        // Should default to 0 VAT when no profile
        assertEquals(BigDecimal.ZERO.setScale(2), result.getVatOrGst());
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
        when(countryProfileRepository.findByCountryCode("SG")).thenReturn(sgProfile);
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("200.00"));
        when(additionalDutyMapRepository
                .findFirstByImportingCountryAndExportingCountryAndHsCodeAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
                        anyString(), anyString(), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // when
        TariffCalculationResultDTO result = tariffCalculatorService.calculate(validRequest);

        // then
        assertNotNull(result);
        assertNull(result.getDate()); // Date should be null when no tariff rate found
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
        when(countryProfileRepository.findByCountryCode("SG")).thenReturn(sgProfile);
        when(shippingCostService.calculateShippingCost(any())).thenReturn(new BigDecimal("200.00"));
        when(additionalDutyMapRepository
                .findFirstByImportingCountryAndExportingCountryAndHsCodeAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
                        anyString(), anyString(), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.empty());

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