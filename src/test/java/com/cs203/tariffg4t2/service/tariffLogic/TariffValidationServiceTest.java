package com.cs203.tariffg4t2.service.tariffLogic;

import com.cs203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.cs203.tariffg4t2.model.basic.Country;
import com.cs203.tariffg4t2.repository.basic.CountryRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TariffValidationServiceTest {

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private TariffValidationService validationService;

    private TariffCalculationRequestDTO testRequest;
    private Country usCountry;
    private Country cnCountry;

    @BeforeEach
    void setUp() {
        testRequest = new TariffCalculationRequestDTO();
        testRequest.setImportingCountry("US");
        testRequest.setExportingCountry("CN");
        testRequest.setHsCode("123456");
        testRequest.setProductValue(new BigDecimal("1000"));
        testRequest.setShippingMode("AIR");
        testRequest.setWeight(new BigDecimal("100"));

        usCountry = new Country();
        usCountry.setCountryCode("US");
        usCountry.setCountryName("United States");

        cnCountry = new Country();
        cnCountry.setCountryCode("CN");
        cnCountry.setCountryName("China");
    }

    @Test
    void validateTariffRequest_ValidRequest_NoErrors() {
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertTrue(errors.isEmpty());
        assertNotNull(testRequest.getMissingFields());
        assertNotNull(testRequest.getDefaultedFields());
    }

    @Test
    void validateTariffRequest_NullRequest_ReturnsError() {
        List<String> errors = validationService.validateTariffRequest(null);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("cannot be null"));
    }

    @Test
    void validateTariffRequest_MissingImportingCountry_ReturnsError() {
        testRequest.setImportingCountry(null);

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Importing country is required")));
        assertTrue(testRequest.getMissingFields().contains("importingCountry"));
    }

    @Test
    void validateTariffRequest_BlankImportingCountry_ReturnsError() {
        testRequest.setImportingCountry("   ");

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Importing country is required")));
    }

    @Test
    void validateTariffRequest_MissingExportingCountry_ReturnsError() {
        testRequest.setExportingCountry("");

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Exporting country is required")));
        assertTrue(testRequest.getMissingFields().contains("exportingCountry"));
    }

    @Test
    void validateTariffRequest_MissingHsCode_ReturnsError() {
        testRequest.setHsCode(null);
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("HS code is required")));
        assertTrue(testRequest.getMissingFields().contains("hsCode"));
    }

    @Test
    void validateTariffRequest_NullProductValue_SetsDefault() {
        testRequest.setProductValue(null);
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertEquals(0, testRequest.getProductValue().compareTo(new BigDecimal("100.00")));
        assertTrue(testRequest.getMissingFields().contains("productValue"));
        assertTrue(testRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("productValue") && f.contains("defaulted")));
    }

    @Test
    void validateTariffRequest_NegativeProductValue_CorrectToDefault() {
        testRequest.setProductValue(new BigDecimal("-50"));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertEquals(0, testRequest.getProductValue().compareTo(new BigDecimal("100.00")));
        assertTrue(testRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("productValue") && f.contains("corrected")));
    }

    @Test
    void validateTariffRequest_ZeroProductValue_CorrectToDefault() {
        testRequest.setProductValue(BigDecimal.ZERO);
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertEquals(0, testRequest.getProductValue().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void validateTariffRequest_MissingShippingMode_SetsDefault() {
        testRequest.setShippingMode(null);
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertEquals("SEA", testRequest.getShippingMode());
        assertTrue(testRequest.getMissingFields().contains("shippingMode"));
        assertTrue(testRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("shippingMode") && f.contains("SEA")));
    }

    @Test
    void validateTariffRequest_MissingHeadsAndWeight_SetsDefaults() {
        testRequest.setHeads(null);
        testRequest.setWeight(null);
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertEquals(1, testRequest.getHeads());
        assertEquals(BigDecimal.ONE, testRequest.getWeight());
        assertTrue(testRequest.getMissingFields().contains("heads/weight"));
        assertTrue(testRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("heads") && f.contains("defaulted")));
        assertTrue(testRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("weight") && f.contains("defaulted")));
    }

    @Test
    void validateTariffRequest_ZeroHeadsAndZeroWeight_SetsDefaults() {
        testRequest.setHeads(0);
        testRequest.setWeight(BigDecimal.ZERO);
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertEquals(1, testRequest.getHeads());
        assertEquals(BigDecimal.ONE, testRequest.getWeight());
    }

    @Test
    void validateTariffRequest_NegativeHeads_CorrectToDefault() {
        testRequest.setHeads(-5);
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertEquals(1, testRequest.getHeads());
        assertTrue(testRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("heads") && f.contains("corrected")));
    }

    @Test
    void validateTariffRequest_NegativeWeight_CorrectToDefault() {
        testRequest.setWeight(new BigDecimal("-10"));
        testRequest.setHeads(5); // Provide valid heads so weight correction branch is tested
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertEquals(0, testRequest.getWeight().compareTo(BigDecimal.ONE));
        assertTrue(testRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("weight") && f.contains("corrected")));
    }

    @Test
    void validateTariffRequest_ValidHeadsOnly_NoWeightDefault() {
        testRequest.setHeads(5);
        testRequest.setWeight(null);
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertEquals(5, testRequest.getHeads());
        assertNull(testRequest.getWeight());
    }

    @Test
    void validateTariffRequest_ValidWeightOnly_NoHeadsDefault() {
        testRequest.setHeads(null);
        testRequest.setWeight(new BigDecimal("50"));
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertNull(testRequest.getHeads());
        assertEquals(new BigDecimal("50"), testRequest.getWeight());
    }

    @Test
    void validateTariffRequest_NullFreight_SetsDefault() {
        testRequest.setFreight(null);
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertEquals(BigDecimal.ZERO, testRequest.getFreight());
        assertTrue(testRequest.getMissingFields().contains("freight"));
        assertTrue(testRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("freight")));
    }

    @Test
    void validateTariffRequest_NullInsurance_SetsDefault() {
        testRequest.setInsurance(null);
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertEquals(BigDecimal.ZERO, testRequest.getInsurance());
        assertTrue(testRequest.getMissingFields().contains("insurance"));
        assertTrue(testRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("insurance")));
    }

    @Test
    void validateTariffRequest_UnknownImportingCountry_ReturnsError() {
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.empty());
        when(countryRepository.findByCountryNameIgnoreCase("US")).thenReturn(Optional.empty());
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Unknown importing country")));
    }

    @Test
    void validateTariffRequest_UnknownExportingCountry_ReturnsError() {
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.empty());
        when(countryRepository.findByCountryNameIgnoreCase("CN")).thenReturn(Optional.empty());

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Unknown exporting country")));
    }

    @Test
    void validateTariffRequest_CountryNameResolution_Success() {
        testRequest.setImportingCountry("United States");
        testRequest.setExportingCountry("China");
        when(countryRepository.findByCountryNameIgnoreCase("United States")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryNameIgnoreCase("China")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertEquals("US", testRequest.getImportingCountry());
        assertEquals("CN", testRequest.getExportingCountry());
    }

    @Test
    void validateTariffRequest_ValidHsCode_NoError() {
        testRequest.setHsCode("123456");
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertEquals("123456", testRequest.getHsCode());
    }

    @Test
    void validateTariffRequest_HsCodeWithDots_CleanedSuccessfully() {
        testRequest.setHsCode("12.34.56");
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertEquals("123456", testRequest.getHsCode());
        assertTrue(testRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("hsCode") && f.contains("cleaned")));
    }

    @Test
    void validateTariffRequest_HsCodeWithSpaces_CleanedSuccessfully() {
        testRequest.setHsCode("12 34 56 78");
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertEquals("12345678", testRequest.getHsCode());
        assertTrue(testRequest.getDefaultedFields().stream()
                .anyMatch(f -> f.contains("hsCode") && f.contains("cleaned")));
    }

    @Test
    void validateTariffRequest_InvalidHsCodeTooShort_ReturnsError() {
        testRequest.setHsCode("12345");
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Invalid HS code format")));
    }

    @Test
    void validateTariffRequest_InvalidHsCodeTooLong_ReturnsError() {
        testRequest.setHsCode("12345678901");
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Invalid HS code format")));
    }

    @Test
    void validateTariffRequest_InvalidHsCodeWithLetters_ReturnsError() {
        testRequest.setHsCode("ABC123");
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        List<String> errors = validationService.validateTariffRequest(testRequest);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Invalid HS code format")));
    }

    @Test
    void resolveToAlpha2_ValidAlpha2Code_ReturnsCode() {
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));

        Optional<String> result = validationService.resolveToAlpha2("US");

        assertTrue(result.isPresent());
        assertEquals("US", result.get());
    }

    @Test
    void resolveToAlpha2_ValidCountryName_ReturnsCode() {
        when(countryRepository.findByCountryNameIgnoreCase("United States")).thenReturn(Optional.of(usCountry));

        Optional<String> result = validationService.resolveToAlpha2("United States");

        assertTrue(result.isPresent());
        assertEquals("US", result.get());
    }

    @Test
    void resolveToAlpha2_NullInput_ReturnsEmpty() {
        Optional<String> result = validationService.resolveToAlpha2(null);

        assertFalse(result.isPresent());
    }

    @Test
    void resolveToAlpha2_BlankInput_ReturnsEmpty() {
        Optional<String> result = validationService.resolveToAlpha2("   ");

        assertFalse(result.isPresent());
    }

    @Test
    void resolveToAlpha2_UnknownCountry_ReturnsEmpty() {
        when(countryRepository.findByCountryCodeIgnoreCase("XX")).thenReturn(Optional.empty());
        when(countryRepository.findByCountryNameIgnoreCase("XX")).thenReturn(Optional.empty());

        Optional<String> result = validationService.resolveToAlpha2("XX");

        assertFalse(result.isPresent());
    }

    @Test
    void isValidRequest_ValidRequest_ReturnsTrue() {
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        boolean result = validationService.isValidRequest(testRequest);

        assertTrue(result);
    }

    @Test
    void isValidRequest_InvalidRequest_ReturnsFalse() {
        testRequest.setHsCode(null);
        when(countryRepository.findByCountryCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCodeIgnoreCase("CN")).thenReturn(Optional.of(cnCountry));

        boolean result = validationService.isValidRequest(testRequest);

        assertFalse(result);
    }
}

