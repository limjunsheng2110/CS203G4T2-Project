package com.cs203.tariffg4t2.service.tariffLogic;

import com.cs203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.cs203.tariffg4t2.dto.response.TariffCalculationResultDTO;
import com.cs203.tariffg4t2.model.basic.Country;
import com.cs203.tariffg4t2.model.basic.TariffRate;
import com.cs203.tariffg4t2.repository.basic.CountryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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

    private TariffCalculationRequestDTO testRequest;
    private Country testCountry;
    private TariffRate testTariffRate;

    @BeforeEach
    void setUp() {
        testRequest = new TariffCalculationRequestDTO();
        testRequest.setImportingCountry("US");
        testRequest.setExportingCountry("CN");
        testRequest.setHsCode("123456");
        testRequest.setProductValue(new BigDecimal("1000"));
        testRequest.setWeight(new BigDecimal("10"));
        testRequest.setFreight(new BigDecimal("50"));
        testRequest.setInsurance(new BigDecimal("25"));
        testRequest.setShippingMode("SEA");
        testRequest.setYear(2024);
        testRequest.setMissingFields(new ArrayList<>());
        testRequest.setDefaultedFields(new ArrayList<>());

        testCountry = new Country();
        testCountry.setCountryCode("US");
        testCountry.setCountryName("United States");
        testCountry.setVatRate(new BigDecimal("10"));

        testTariffRate = new TariffRate();
        testTariffRate.setId(1L);
        testTariffRate.setHsCode("123456");
        testTariffRate.setImportingCountryCode("US");
        testTariffRate.setExportingCountryCode("CN");
        testTariffRate.setAdValoremRate(new BigDecimal("7.5"));
        testTariffRate.setYear(2024);
    }

    @Test
    void calculate_WithValidRequest_ReturnsCompleteResult() {
        when(tariffValidationService.validateTariffRequest(testRequest)).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(testRequest)).thenReturn(new BigDecimal("75.00"));
        when(tariffRateService.getTariffRateWithYear(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Optional.of(testTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("7.5"));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(testCountry));
        when(shippingCostService.calculateShippingCost(testRequest)).thenReturn(new BigDecimal("100.00"));
        when(shippingCostService.getShippingRatePerKg(testRequest)).thenReturn(new BigDecimal("10.00"));

        TariffCalculationResultDTO result = tariffCalculatorService.calculate(testRequest);

        assertNotNull(result);
        assertEquals("US", result.getImportingCountry());
        assertEquals("CN", result.getExportingCountry());
        assertEquals("123456", result.getHsCode());
        assertEquals(new BigDecimal("1000.00"), result.getProductValue());
        assertEquals(new BigDecimal("1075.00"), result.getCustomsValue()); // 1000 + 50 + 25
        // BaseDuty is scaled: 75 * (1075/1000) = 80.625, rounded to 80.63
        assertEquals(new BigDecimal("80.63"), result.getBaseDuty());
        // VAT is (1075 + 80.63) * 0.10 = 115.563, rounded to 115.56
        assertEquals(new BigDecimal("115.56"), result.getVatOrGst());
        assertEquals(new BigDecimal("100.00"), result.getShippingCost());
        // Total: 1075 + 80.63 + 115.56 + 100 = 1371.19
        assertEquals(new BigDecimal("1371.19"), result.getTotalCost());
        assertEquals(new BigDecimal("10"), result.getVatRate());
        assertEquals(2024, result.getYear());
    }

    @Test
    void calculate_WithVatOverride_UsesOverrideValue() {
        testRequest.setVatOrGstOverride(new BigDecimal("0.15")); // 15%

        when(tariffValidationService.validateTariffRequest(testRequest)).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(testRequest)).thenReturn(new BigDecimal("75.00"));
        when(tariffRateService.getTariffRateWithYear(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Optional.of(testTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("7.5"));
        when(shippingCostService.calculateShippingCost(testRequest)).thenReturn(new BigDecimal("100.00"));
        when(shippingCostService.getShippingRatePerKg(testRequest)).thenReturn(new BigDecimal("10.00"));

        TariffCalculationResultDTO result = tariffCalculatorService.calculate(testRequest);

        assertNotNull(result);
        // BaseDuty scaled: 75 * (1075/1000) = 80.625, rounded to 80.63
        // VAT: (1075 + 80.63) * 0.15 = 173.3445, rounded to 173.34
        assertEquals(new BigDecimal("173.34"), result.getVatOrGst());
        assertEquals(new BigDecimal("15.00"), result.getVatRate()); // 15%
        verify(countryRepository, never()).findByCountryCodeIgnoreCase(anyString());
    }

    @Test
    void calculate_WithNoVatRate_UsesZeroVat() {
        testCountry.setVatRate(null);

        when(tariffValidationService.validateTariffRequest(testRequest)).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(testRequest)).thenReturn(new BigDecimal("75.00"));
        when(tariffRateService.getTariffRateWithYear(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Optional.of(testTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("7.5"));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(testCountry));
        when(shippingCostService.calculateShippingCost(testRequest)).thenReturn(new BigDecimal("100.00"));
        when(shippingCostService.getShippingRatePerKg(testRequest)).thenReturn(new BigDecimal("10.00"));

        TariffCalculationResultDTO result = tariffCalculatorService.calculate(testRequest);

        assertNotNull(result);
        assertEquals(new BigDecimal("0.00"), result.getVatOrGst());
        assertEquals(new BigDecimal("0"), result.getVatRate());
    }

    @Test
    void calculate_WithCountryNotFound_UsesZeroVat() {
        when(tariffValidationService.validateTariffRequest(testRequest)).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(testRequest)).thenReturn(new BigDecimal("75.00"));
        when(tariffRateService.getTariffRateWithYear(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Optional.of(testTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("7.5"));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.empty());
        when(shippingCostService.calculateShippingCost(testRequest)).thenReturn(new BigDecimal("100.00"));
        when(shippingCostService.getShippingRatePerKg(testRequest)).thenReturn(new BigDecimal("10.00"));

        TariffCalculationResultDTO result = tariffCalculatorService.calculate(testRequest);

        assertNotNull(result);
        assertEquals(new BigDecimal("0.00"), result.getVatOrGst());
        assertEquals(new BigDecimal("0"), result.getVatRate());
    }

    @Test
    void calculate_WithNullValues_HandlesGracefully() {
        testRequest.setProductValue(null);
        testRequest.setFreight(null);
        testRequest.setInsurance(null);

        when(tariffValidationService.validateTariffRequest(testRequest)).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(testRequest)).thenReturn(BigDecimal.ZERO);
        when(tariffRateService.getTariffRateWithYear(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Optional.empty());
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(BigDecimal.ZERO);
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(testCountry));
        when(shippingCostService.calculateShippingCost(testRequest)).thenReturn(BigDecimal.ZERO);
        when(shippingCostService.getShippingRatePerKg(testRequest)).thenReturn(BigDecimal.ZERO);

        TariffCalculationResultDTO result = tariffCalculatorService.calculate(testRequest);

        assertNotNull(result);
        assertEquals(new BigDecimal("0.00"), result.getCustomsValue());
        assertEquals(new BigDecimal("0.00"), result.getBaseDuty());
        assertEquals(new BigDecimal("0.00"), result.getTotalCost());
    }

    @Test
    void calculate_WithValidationErrors_ThrowsException() {
        ArrayList<String> errors = new ArrayList<>();
        errors.add("Importing country is required");

        when(tariffValidationService.validateTariffRequest(testRequest)).thenReturn(errors);

        assertThrows(IllegalArgumentException.class, () -> {
            tariffCalculatorService.calculate(testRequest);
        });
    }

    @Test
    void calculate_WithMissingFields_LogsAndContinues() {
        testRequest.getMissingFields().add("weight");
        testRequest.getDefaultedFields().add("weight (defaulted to 1.0 kg)");

        when(tariffValidationService.validateTariffRequest(testRequest)).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(testRequest)).thenReturn(new BigDecimal("75.00"));
        when(tariffRateService.getTariffRateWithYear(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Optional.of(testTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("7.5"));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(testCountry));
        when(shippingCostService.calculateShippingCost(testRequest)).thenReturn(new BigDecimal("100.00"));
        when(shippingCostService.getShippingRatePerKg(testRequest)).thenReturn(new BigDecimal("10.00"));

        TariffCalculationResultDTO result = tariffCalculatorService.calculate(testRequest);

        assertNotNull(result);
        assertFalse(testRequest.getMissingFields().isEmpty());
        assertFalse(testRequest.getDefaultedFields().isEmpty());
    }

    @Test
    void calculate_WithZeroProductValue_ScalesCorrectly() {
        testRequest.setProductValue(BigDecimal.ZERO);

        when(tariffValidationService.validateTariffRequest(testRequest)).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(testRequest)).thenReturn(BigDecimal.ZERO);
        when(tariffRateService.getTariffRateWithYear(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Optional.of(testTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("7.5"));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(testCountry));
        when(shippingCostService.calculateShippingCost(testRequest)).thenReturn(new BigDecimal("100.00"));
        when(shippingCostService.getShippingRatePerKg(testRequest)).thenReturn(new BigDecimal("10.00"));

        TariffCalculationResultDTO result = tariffCalculatorService.calculate(testRequest);

        assertNotNull(result);
        assertEquals(new BigDecimal("0.00"), result.getProductValue());
        assertEquals(new BigDecimal("75.00"), result.getCustomsValue()); // 0 + 50 + 25
    }

    @Test
    void calculate_WithNullYear_ReturnsNullYearInResult() {
        testRequest.setYear(null);

        when(tariffValidationService.validateTariffRequest(testRequest)).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(testRequest)).thenReturn(new BigDecimal("75.00"));
        when(tariffRateService.getTariffRateWithYear(anyString(), anyString(), anyString(), isNull()))
                .thenReturn(Optional.empty());
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("7.5"));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(testCountry));
        when(shippingCostService.calculateShippingCost(testRequest)).thenReturn(new BigDecimal("100.00"));
        when(shippingCostService.getShippingRatePerKg(testRequest)).thenReturn(new BigDecimal("10.00"));

        TariffCalculationResultDTO result = tariffCalculatorService.calculate(testRequest);

        assertNotNull(result);
        assertNull(result.getYear());
    }

    @Test
    void calculate_WithHighVatRate_CalculatesCorrectly() {
        testCountry.setVatRate(new BigDecimal("25")); // 25% VAT

        when(tariffValidationService.validateTariffRequest(testRequest)).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(testRequest)).thenReturn(new BigDecimal("75.00"));
        when(tariffRateService.getTariffRateWithYear(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Optional.of(testTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("7.5"));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(testCountry));
        when(shippingCostService.calculateShippingCost(testRequest)).thenReturn(new BigDecimal("100.00"));
        when(shippingCostService.getShippingRatePerKg(testRequest)).thenReturn(new BigDecimal("10.00"));

        TariffCalculationResultDTO result = tariffCalculatorService.calculate(testRequest);

        assertNotNull(result);
        // BaseDuty scaled: 75 * (1075/1000) = 80.625, rounded to 80.63
        // VAT: (1075 + 80.63) * 0.25 = 288.9075, rounded to 288.91
        assertEquals(new BigDecimal("288.91"), result.getVatOrGst());
        assertEquals(new BigDecimal("25"), result.getVatRate());
    }

    @Test
    void calculate_WithNullWeight_IncludesInResult() {
        testRequest.setWeight(null);

        when(tariffValidationService.validateTariffRequest(testRequest)).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(testRequest)).thenReturn(new BigDecimal("75.00"));
        when(tariffRateService.getTariffRateWithYear(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Optional.of(testTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("7.5"));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(testCountry));
        when(shippingCostService.calculateShippingCost(testRequest)).thenReturn(BigDecimal.ZERO);
        when(shippingCostService.getShippingRatePerKg(testRequest)).thenReturn(BigDecimal.ZERO);

        TariffCalculationResultDTO result = tariffCalculatorService.calculate(testRequest);

        assertNotNull(result);
        assertNull(result.getTotalWeight());
    }

    @Test
    void calculate_WithNullHeads_HandlesGracefully() {
        testRequest.setHeads(null);

        when(tariffValidationService.validateTariffRequest(testRequest)).thenReturn(new ArrayList<>());
        when(tariffRateService.calculateTariffAmount(testRequest)).thenReturn(new BigDecimal("75.00"));
        when(tariffRateService.getTariffRateWithYear(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Optional.of(testTariffRate));
        when(tariffRateService.getAdValoremRate(anyString(), anyString(), anyString()))
                .thenReturn(new BigDecimal("7.5"));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(testCountry));
        when(shippingCostService.calculateShippingCost(testRequest)).thenReturn(new BigDecimal("100.00"));
        when(shippingCostService.getShippingRatePerKg(testRequest)).thenReturn(new BigDecimal("10.00"));

        TariffCalculationResultDTO result = tariffCalculatorService.calculate(testRequest);

        assertNotNull(result);
        assertNull(result.getHeads());
    }
}
