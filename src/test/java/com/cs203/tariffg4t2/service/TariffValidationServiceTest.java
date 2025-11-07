package com.cs203.tariffg4t2.service;

import com.cs203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.cs203.tariffg4t2.model.basic.Country;
import com.cs203.tariffg4t2.repository.basic.CountryRepository;
import com.cs203.tariffg4t2.service.tariffLogic.TariffValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TariffValidationServiceTest {

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private TariffValidationService tariffValidationService;

    private TariffCalculationRequestDTO validRequest;
    private Country sgCountry;
    private Country usCountry;

    @BeforeEach
    void setUp() {
        // Setup valid request
        validRequest = new TariffCalculationRequestDTO();
        validRequest.setImportingCountry("SG");
        validRequest.setExportingCountry("US");
        validRequest.setHsCode("010329");
        validRequest.setProductValue(new BigDecimal("1000.00"));
        validRequest.setFreight(new BigDecimal("100.00"));
        validRequest.setInsurance(new BigDecimal("50.00"));
        validRequest.setHeads(10);
        validRequest.setWeight(new BigDecimal("500.00"));
        validRequest.setRooEligible(false);
        validRequest.setShippingMode("SEA");

        // Setup countries
        sgCountry = new Country("SG", "Singapore", "SGP");
        usCountry = new Country("US", "United States", "USA");
    }

    // null request test

    @Test
    void testValidateTariffRequest_NullRequest() {
        // when
        List<String> errors = tariffValidationService.validateTariffRequest(null);

        // then
        assertNotNull(errors);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Request cannot be null"));
    }

    // valid requests test

    @Test
    void testValidateTariffRequest_Success() {
        // given
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertTrue(errors.isEmpty());
        assertEquals("SG", validRequest.getImportingCountry());
        assertEquals("US", validRequest.getExportingCountry());
    }

    @Test
    void testIsValidRequest_True() {
        // given
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        // when
        boolean result = tariffValidationService.isValidRequest(validRequest);

        // then
        assertTrue(result);
    }

    @Test
    void testIsValidRequest_False() {
        // given
        validRequest.setImportingCountry(null);

        // when
        boolean result = tariffValidationService.isValidRequest(validRequest);

        // then
        assertFalse(result);
    }

    // tests for missing required fields: importingCountry, exportingCountry, hsCode

    @Test
    void testValidateTariffRequest_MissingImportingCountry() {
        // given
        validRequest.setImportingCountry(null);

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Importing country is required")));
        assertTrue(validRequest.getMissingFields().contains("importingCountry"));
    }

    @Test
    void testValidateTariffRequest_BlankImportingCountry() {
        // given
        validRequest.setImportingCountry("   ");

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Importing country is required")));
    }

    @Test
    void testValidateTariffRequest_MissingExportingCountry() {
        // given
        validRequest.setExportingCountry(null);
        lenient().when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry)); // Add lenient()

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Exporting country is required")));
        assertTrue(validRequest.getMissingFields().contains("exportingCountry"));
    }

    @Test
    void testValidateTariffRequest_MissingHsCode() {
        // given
        validRequest.setHsCode(null);
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("HS code is required")));
        assertTrue(validRequest.getMissingFields().contains("hsCode"));
    }

    @Test
    void testValidateTariffRequest_BlankHsCode() {
        // given
        validRequest.setHsCode("   ");
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("HS code is required")));
    }

    // ========== DEFAULTED FIELDS TESTS ==========

    @Test
    void testValidateTariffRequest_DefaultProductValue() {
        // given
        validRequest.setProductValue(null);
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertTrue(errors.isEmpty());
        assertEquals(0, new BigDecimal("100.00").compareTo(validRequest.getProductValue()));
        assertTrue(validRequest.getMissingFields().contains("productValue"));
        assertTrue(validRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("productValue") && f.contains("$100.00")));
    }

    @Test
    void testValidateTariffRequest_NegativeProductValue() {
        // given
        validRequest.setProductValue(new BigDecimal("-50.00"));
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertTrue(errors.isEmpty());
        assertEquals(0, new BigDecimal("100.00").compareTo(validRequest.getProductValue()));
        assertTrue(validRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("corrected from non-positive")));
    }

    @Test
    void testValidateTariffRequest_ZeroProductValue() {
        // given
        validRequest.setProductValue(BigDecimal.ZERO);
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertTrue(errors.isEmpty());
        assertEquals(0, new BigDecimal("100.00").compareTo(validRequest.getProductValue()));
    }

    @Test
    void testValidateTariffRequest_DefaultRooEligible() {
        // given
        validRequest.setRooEligible(null);
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertTrue(errors.isEmpty());
        assertFalse(validRequest.getRooEligible());
        assertTrue(validRequest.getMissingFields().contains("rooEligible"));
        assertTrue(validRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("rooEligible") && f.contains("false")));
    }

    @Test
    void testValidateTariffRequest_DefaultShippingMode() {
        // given
        validRequest.setShippingMode(null);
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertTrue(errors.isEmpty());
        assertEquals("SEA", validRequest.getShippingMode());
        assertTrue(validRequest.getMissingFields().contains("shippingMode"));
        assertTrue(validRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("shippingMode") && f.contains("SEA")));
    }

    @Test
    void testValidateTariffRequest_DefaultFreight() {
        // given
        validRequest.setFreight(null);
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertTrue(errors.isEmpty());
        assertEquals(BigDecimal.ZERO, validRequest.getFreight());
        assertTrue(validRequest.getMissingFields().contains("freight"));
        assertTrue(validRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("freight") && f.contains("$0.00")));
    }

    @Test
    void testValidateTariffRequest_DefaultInsurance() {
        // given
        validRequest.setInsurance(null);
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertTrue(errors.isEmpty());
        assertEquals(BigDecimal.ZERO, validRequest.getInsurance());
        assertTrue(validRequest.getMissingFields().contains("insurance"));
        assertTrue(validRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("insurance") && f.contains("$0.00")));
    }

    @Test
    void testValidateTariffRequest_DefaultHeadsAndWeight() {
        // given
        validRequest.setHeads(null);
        validRequest.setWeight(null);
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertTrue(errors.isEmpty());
        assertEquals(1, validRequest.getHeads());
        assertEquals(BigDecimal.ONE, validRequest.getWeight());
        assertTrue(validRequest.getMissingFields().contains("heads/weight"));
        assertTrue(validRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("heads") && f.contains("1")));
        assertTrue(validRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("weight") && f.contains("1.0 kg")));
    }

    @Test
    void testValidateTariffRequest_NegativeHeads() {
        // given
        validRequest.setHeads(-5);
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertTrue(errors.isEmpty());
        assertEquals(1, validRequest.getHeads());
        assertTrue(validRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("heads") && f.contains("corrected from negative")));
    }

    @Test
    void testValidateTariffRequest_NegativeWeight() {
        // given
        validRequest.setWeight(new BigDecimal("-10.00"));
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertTrue(errors.isEmpty());
        assertEquals(BigDecimal.ONE, validRequest.getWeight());
        assertTrue(validRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("weight") && f.contains("corrected from negative")));
    }

    // tests for country validation by code and name, including case insensitivity

    @Test
    void testValidateTariffRequest_InvalidImportingCountry() {
        // given
        validRequest.setImportingCountry("XX");
        when(countryRepository.findByCountryCodeIgnoreCase("XX")).thenReturn(Optional.empty());
        when(countryRepository.findByCountryNameIgnoreCase("XX")).thenReturn(Optional.empty());
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Unknown importing country")));
    }

    @Test
    void testValidateTariffRequest_InvalidExportingCountry() {
        // given
        validRequest.setExportingCountry("YY");
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("YY")).thenReturn(Optional.empty());
        when(countryRepository.findByCountryNameIgnoreCase("YY")).thenReturn(Optional.empty());

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Unknown exporting country")));
    }

    @Test
    void testValidateTariffRequest_CountryByName() {
        // given
        validRequest.setImportingCountry("Singapore");
        validRequest.setExportingCountry("United States");
        when(countryRepository.findByCountryNameIgnoreCase("Singapore")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryNameIgnoreCase("United States")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertTrue(errors.isEmpty());
        assertEquals("SG", validRequest.getImportingCountry());
        assertEquals("US", validRequest.getExportingCountry());
    }

    @Test
    void testValidateTariffRequest_CountryCodeCaseInsensitive() {
        // given
        validRequest.setImportingCountry("sg");
        validRequest.setExportingCountry("us");
        when(countryRepository.findByCountryCodeIgnoreCase("sg")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("us")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertTrue(errors.isEmpty());
        assertEquals("SG", validRequest.getImportingCountry());
        assertEquals("US", validRequest.getExportingCountry());
    }

    // tests for valid and invalid HS codes, including cleaning non-digit characters

    @Test
    void testValidateTariffRequest_ValidHsCode6Digits() {
        // given
        validRequest.setHsCode("123456");
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertTrue(errors.isEmpty());
        assertEquals("123456", validRequest.getHsCode());
    }

    @Test
    void testValidateTariffRequest_ValidHsCode10Digits() {
        // given
        validRequest.setHsCode("1234567890");
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertTrue(errors.isEmpty());
        assertEquals("1234567890", validRequest.getHsCode());
    }

    @Test
    void testValidateTariffRequest_HsCodeWithNonDigits() {
        // given
        validRequest.setHsCode("12-34.56");
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertTrue(errors.isEmpty());
        assertEquals("123456", validRequest.getHsCode());
        assertTrue(validRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("hsCode") && f.contains("cleaned non-digit")));
    }

    @Test
    void testValidateTariffRequest_InvalidHsCodeTooShort() {
        // given
        validRequest.setHsCode("12345");
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Invalid HS code format")));
    }

    @Test
    void testValidateTariffRequest_InvalidHsCodeTooLong() {
        // given
        validRequest.setHsCode("12345678901");
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Invalid HS code format")));
    }

    @Test
    void testValidateTariffRequest_HsCodeWithLetters() {
        // given
        validRequest.setHsCode("12AB34");
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(validRequest);

        // then
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Invalid HS code format")));
    }

    // tests for resolve to alpha2

    @Test
    void testResolveToAlpha2_ByCode() {
        // given
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));

        // when
        Optional<String> result = tariffValidationService.resolveToAlpha2("SG");

        // then
        assertTrue(result.isPresent());
        assertEquals("SG", result.get());
    }

    @Test
    void testResolveToAlpha2_ByName() {
        // given
        when(countryRepository.findByCountryNameIgnoreCase("Singapore")).thenReturn(Optional.of(sgCountry));

        // when
        Optional<String> result = tariffValidationService.resolveToAlpha2("Singapore");

        // then
        assertTrue(result.isPresent());
        assertEquals("SG", result.get());
    }

    @Test
    void testResolveToAlpha2_NotFound() {
        // given
        when(countryRepository.findByCountryCodeIgnoreCase("XX")).thenReturn(Optional.empty());
        when(countryRepository.findByCountryNameIgnoreCase("XX")).thenReturn(Optional.empty());

        // when
        Optional<String> result = tariffValidationService.resolveToAlpha2("XX");

        // then
        assertFalse(result.isPresent());
    }

    @Test
    void testResolveToAlpha2_NullInput() {
        // when
        Optional<String> result = tariffValidationService.resolveToAlpha2(null);

        // then
        assertFalse(result.isPresent());
    }

    @Test
    void testResolveToAlpha2_BlankInput() {
        // when
        Optional<String> result = tariffValidationService.resolveToAlpha2("   ");

        // then
        assertFalse(result.isPresent());
    }

    @Test
    void testResolveToAlpha2_EmptyInput() {
        // when
        Optional<String> result = tariffValidationService.resolveToAlpha2("");

        // then
        assertFalse(result.isPresent());
    }

    // tests for multiple validation errors in a single request

    @Test
    void testValidateTariffRequest_MultipleErrors() {
        // given
        TariffCalculationRequestDTO request = new TariffCalculationRequestDTO();
        request.setImportingCountry(null);
        request.setExportingCountry(null);
        request.setHsCode(null);

        // when
        List<String> errors = tariffValidationService.validateTariffRequest(request);

        // then
        assertEquals(3, errors.size());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Importing country is required")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("Exporting country is required")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("HS code is required")));
    }

    // ========== CACHE TEST ==========

    @Test
    void testResolveToAlpha2_MultipleCalls() {
        // given
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));

        // when - call twice
        Optional<String> result1 = tariffValidationService.resolveToAlpha2("SG");
        Optional<String> result2 = tariffValidationService.resolveToAlpha2("SG");

        // then - service doesn't cache, so repository is called twice
        assertTrue(result1.isPresent());
        assertTrue(result2.isPresent());
        assertEquals("SG", result1.get());
        assertEquals("SG", result2.get());
        verify(countryRepository, times(2)).findByCountryCodeIgnoreCase("SG");
    }
}