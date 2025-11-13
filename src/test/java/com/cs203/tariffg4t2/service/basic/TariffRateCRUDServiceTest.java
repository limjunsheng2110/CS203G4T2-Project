package com.cs203.tariffg4t2.service.basic;

import com.cs203.tariffg4t2.dto.basic.TariffRateDTO;
import com.cs203.tariffg4t2.model.basic.TariffRate;
import com.cs203.tariffg4t2.repository.basic.CountryRepository;
import com.cs203.tariffg4t2.repository.basic.TariffRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TariffRateCRUDServiceTest {

    @Mock
    private TariffRateRepository tariffRateRepository;

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private TariffRateCRUDService tariffRateCRUDService;

    private TariffRateDTO validDTO;
    private TariffRate testTariffRate;

    @BeforeEach
    void setUp() {
        validDTO = new TariffRateDTO();
        validDTO.setHsCode("123456");
        validDTO.setImportingCountryCode("US");
        validDTO.setExportingCountryCode("CN");
        validDTO.setBaseRate(new BigDecimal("10.5"));
        validDTO.setYear(2024);

        testTariffRate = new TariffRate();
        testTariffRate.setId(1L);
        testTariffRate.setHsCode("123456");
        testTariffRate.setImportingCountryCode("US");
        testTariffRate.setExportingCountryCode("CN");
        testTariffRate.setAdValoremRate(new BigDecimal("10.5"));
        testTariffRate.setYear(2024);
    }

    // Create Tests
    @Test
    void createTariffRate_ValidDTO_Success() {
        when(countryRepository.existsByCountryCodeIgnoreCase("US")).thenReturn(true);
        when(countryRepository.existsByCountryCodeIgnoreCase("CN")).thenReturn(true);
        when(tariffRateRepository.save(any(TariffRate.class))).thenReturn(testTariffRate);

        TariffRate result = tariffRateCRUDService.createTariffRate(validDTO);

        assertNotNull(result);
        assertEquals("123456", result.getHsCode());
        verify(tariffRateRepository, times(1)).save(any(TariffRate.class));
    }

    @Test
    void createTariffRate_NullDTO_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            tariffRateCRUDService.createTariffRate(null);
        });
    }

    @Test
    void createTariffRate_NullHsCode_ThrowsException() {
        validDTO.setHsCode(null);

        assertThrows(IllegalArgumentException.class, () -> {
            tariffRateCRUDService.createTariffRate(validDTO);
        });
    }

    @Test
    void createTariffRate_EmptyHsCode_ThrowsException() {
        validDTO.setHsCode("   ");

        assertThrows(IllegalArgumentException.class, () -> {
            tariffRateCRUDService.createTariffRate(validDTO);
        });
    }

    @Test
    void createTariffRate_NullImportingCountry_ThrowsException() {
        validDTO.setImportingCountryCode(null);

        assertThrows(IllegalArgumentException.class, () -> {
            tariffRateCRUDService.createTariffRate(validDTO);
        });
    }

    @Test
    void createTariffRate_EmptyImportingCountry_ThrowsException() {
        validDTO.setImportingCountryCode("");

        assertThrows(IllegalArgumentException.class, () -> {
            tariffRateCRUDService.createTariffRate(validDTO);
        });
    }

    @Test
    void createTariffRate_NullExportingCountry_ThrowsException() {
        validDTO.setExportingCountryCode(null);

        assertThrows(IllegalArgumentException.class, () -> {
            tariffRateCRUDService.createTariffRate(validDTO);
        });
    }

    @Test
    void createTariffRate_EmptyExportingCountry_ThrowsException() {
        validDTO.setExportingCountryCode("  ");

        assertThrows(IllegalArgumentException.class, () -> {
            tariffRateCRUDService.createTariffRate(validDTO);
        });
    }

    @Test
    void createTariffRate_NegativeBaseRate_ThrowsException() {
        validDTO.setBaseRate(new BigDecimal("-5.0"));

        assertThrows(IllegalArgumentException.class, () -> {
            tariffRateCRUDService.createTariffRate(validDTO);
        });
    }

    @Test
    void createTariffRate_ImportingCountryCodeTooShort_ThrowsException() {
        validDTO.setImportingCountryCode("U");

        assertThrows(IllegalArgumentException.class, () -> {
            tariffRateCRUDService.createTariffRate(validDTO);
        });
    }

    @Test
    void createTariffRate_ImportingCountryCodeTooLong_ThrowsException() {
        validDTO.setImportingCountryCode("USAA");

        assertThrows(IllegalArgumentException.class, () -> {
            tariffRateCRUDService.createTariffRate(validDTO);
        });
    }

    @Test
    void createTariffRate_ExportingCountryCodeTooShort_ThrowsException() {
        validDTO.setExportingCountryCode("C");

        assertThrows(IllegalArgumentException.class, () -> {
            tariffRateCRUDService.createTariffRate(validDTO);
        });
    }

    @Test
    void createTariffRate_ExportingCountryCodeTooLong_ThrowsException() {
        validDTO.setExportingCountryCode("CHIN");

        assertThrows(IllegalArgumentException.class, () -> {
            tariffRateCRUDService.createTariffRate(validDTO);
        });
    }

    @Test
    void createTariffRate_ImportingCountryNotExists_ThrowsException() {
        when(countryRepository.existsByCountryCodeIgnoreCase("US")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            tariffRateCRUDService.createTariffRate(validDTO);
        });
    }

    @Test
    void createTariffRate_ExportingCountryNotExists_ThrowsException() {
        when(countryRepository.existsByCountryCodeIgnoreCase("US")).thenReturn(true);
        when(countryRepository.existsByCountryCodeIgnoreCase("CN")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            tariffRateCRUDService.createTariffRate(validDTO);
        });
    }

    // Get All Tests
    @Test
    void getAllTariffRates_ReturnsAllRates() {
        List<TariffRate> rates = Arrays.asList(testTariffRate);
        when(tariffRateRepository.findAll()).thenReturn(rates);

        List<TariffRate> result = tariffRateCRUDService.getAllTariffRates();

        assertEquals(1, result.size());
        assertEquals("123456", result.get(0).getHsCode());
    }

    @Test
    void getAllTariffRates_EmptyList_ReturnsEmpty() {
        when(tariffRateRepository.findAll()).thenReturn(new ArrayList<>());

        List<TariffRate> result = tariffRateCRUDService.getAllTariffRates();

        assertTrue(result.isEmpty());
    }

    // Get By ID Tests
    @Test
    void getTariffRateById_Exists_ReturnsRate() {
        when(tariffRateRepository.findById(1L)).thenReturn(Optional.of(testTariffRate));

        Optional<TariffRate> result = tariffRateCRUDService.getTariffRateById(1L);

        assertTrue(result.isPresent());
        assertEquals("123456", result.get().getHsCode());
    }

    @Test
    void getTariffRateById_NotExists_ReturnsEmpty() {
        when(tariffRateRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<TariffRate> result = tariffRateCRUDService.getTariffRateById(999L);

        assertFalse(result.isPresent());
    }

    // Get By Details Tests (without year)
    @Test
    void getTariffRateByDetails_Found_ReturnsRate() {
        List<TariffRate> rates = Arrays.asList(testTariffRate);
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "123456", "US", "CN")).thenReturn(rates);

        Optional<TariffRate> result = tariffRateCRUDService.getTariffRateByDetails("123456", "US", "CN");

        assertTrue(result.isPresent());
        assertEquals("123456", result.get().getHsCode());
    }

    @Test
    void getTariffRateByDetails_NotFound_ReturnsEmpty() {
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "999999", "JP", "KR")).thenReturn(new ArrayList<>());

        Optional<TariffRate> result = tariffRateCRUDService.getTariffRateByDetails("999999", "JP", "KR");

        assertFalse(result.isPresent());
    }

    @Test
    void getTariffRateByDetails_MultipleYears_ReturnsMostRecent() {
        TariffRate rate2023 = new TariffRate();
        rate2023.setId(2L);
        rate2023.setYear(2023);

        TariffRate rate2024 = new TariffRate();
        rate2024.setId(1L);
        rate2024.setYear(2024);

        List<TariffRate> rates = Arrays.asList(rate2023, rate2024);
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "123456", "US", "CN")).thenReturn(rates);

        Optional<TariffRate> result = tariffRateCRUDService.getTariffRateByDetails("123456", "US", "CN");

        assertTrue(result.isPresent());
        assertEquals(2024, result.get().getYear());
    }

    @Test
    void getTariffRateByDetails_WithNullYears_HandlesCorrectly() {
        TariffRate rateNoYear1 = new TariffRate();
        rateNoYear1.setId(1L);
        rateNoYear1.setYear(null);

        TariffRate rateNoYear2 = new TariffRate();
        rateNoYear2.setId(2L);
        rateNoYear2.setYear(null);

        List<TariffRate> rates = Arrays.asList(rateNoYear1, rateNoYear2);
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "123456", "US", "CN")).thenReturn(rates);

        Optional<TariffRate> result = tariffRateCRUDService.getTariffRateByDetails("123456", "US", "CN");

        assertTrue(result.isPresent());
    }

    @Test
    void getTariffRateByDetails_MixedYears_PrefersNonNull() {
        TariffRate rateWithYear = new TariffRate();
        rateWithYear.setId(1L);
        rateWithYear.setYear(2024);

        TariffRate rateNoYear = new TariffRate();
        rateNoYear.setId(2L);
        rateNoYear.setYear(null);

        List<TariffRate> rates = Arrays.asList(rateNoYear, rateWithYear);
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "123456", "US", "CN")).thenReturn(rates);

        Optional<TariffRate> result = tariffRateCRUDService.getTariffRateByDetails("123456", "US", "CN");

        assertTrue(result.isPresent());
        assertEquals(2024, result.get().getYear());
    }

    // Get All By Details Tests (with year)
    @Test
    void getAllTariffRatesByDetails_Found_ReturnsSortedList() {
        TariffRate rate2023 = new TariffRate();
        rate2023.setId(2L);
        rate2023.setYear(2023);

        TariffRate rate2024 = new TariffRate();
        rate2024.setId(1L);
        rate2024.setYear(2024);

        List<TariffRate> rates = Arrays.asList(rate2023, rate2024);
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "123456", "US", "CN")).thenReturn(rates);

        List<TariffRate> result = tariffRateCRUDService.getAllTariffRatesByDetails("123456", "US", "CN", 2024);

        assertEquals(2, result.size());
        assertEquals(2024, result.get(0).getYear()); // Requested year first
    }

    @Test
    void getAllTariffRatesByDetails_NotFound_ReturnsEmpty() {
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "999999", "JP", "KR")).thenReturn(new ArrayList<>());

        List<TariffRate> result = tariffRateCRUDService.getAllTariffRatesByDetails("999999", "JP", "KR", 2024);

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllTariffRatesByDetails_NullRequestedYear_SortsByYear() {
        TariffRate rate2023 = new TariffRate();
        rate2023.setId(2L);
        rate2023.setYear(2023);

        TariffRate rate2024 = new TariffRate();
        rate2024.setId(1L);
        rate2024.setYear(2024);

        List<TariffRate> rates = Arrays.asList(rate2023, rate2024);
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "123456", "US", "CN")).thenReturn(rates);

        List<TariffRate> result = tariffRateCRUDService.getAllTariffRatesByDetails("123456", "US", "CN", null);

        assertEquals(2, result.size());
        assertEquals(2024, result.get(0).getYear()); // Most recent first
    }

    // Get By Details With Year Tests
    @Test
    void getTariffRateByDetailsWithYear_NullYear_UsesOriginalMethod() {
        List<TariffRate> rates = Arrays.asList(testTariffRate);
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "123456", "US", "CN")).thenReturn(rates);

        Optional<TariffRate> result = tariffRateCRUDService.getTariffRateByDetails("123456", "US", "CN", null);

        assertTrue(result.isPresent());
        verify(tariffRateRepository, times(1)).findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "123456", "US", "CN");
    }

    @Test
    void getTariffRateByDetailsWithYear_ExactMatch_ReturnsExactYear() {
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                "123456", "US", "CN", 2024)).thenReturn(Optional.of(testTariffRate));

        Optional<TariffRate> result = tariffRateCRUDService.getTariffRateByDetails("123456", "US", "CN", 2024);

        assertTrue(result.isPresent());
        assertEquals(2024, result.get().getYear());
    }

    @Test
    void getTariffRateByDetailsWithYear_NoExactMatch_FindsClosest() {
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                "123456", "US", "CN", 2024)).thenReturn(Optional.empty());

        TariffRate closestRate = new TariffRate();
        closestRate.setId(2L);
        closestRate.setYear(2023);

        when(tariffRateRepository.findClosestYearTariffRate("123456", "US", "CN", 2024))
                .thenReturn(Arrays.asList(closestRate));

        Optional<TariffRate> result = tariffRateCRUDService.getTariffRateByDetails("123456", "US", "CN", 2024);

        assertTrue(result.isPresent());
        assertEquals(2023, result.get().getYear());
    }

    @Test
    void getTariffRateByDetailsWithYear_NoMatch_ReturnsEmpty() {
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCodeAndYear(
                "123456", "US", "CN", 2024)).thenReturn(Optional.empty());
        when(tariffRateRepository.findClosestYearTariffRate("123456", "US", "CN", 2024))
                .thenReturn(new ArrayList<>());

        Optional<TariffRate> result = tariffRateCRUDService.getTariffRateByDetails("123456", "US", "CN", 2024);

        assertFalse(result.isPresent());
    }

    // Delete Tests
    @Test
    void deleteTariffRate_Exists_Deletes() {
        when(tariffRateRepository.existsById(1L)).thenReturn(true);
        doNothing().when(tariffRateRepository).deleteById(1L);

        assertDoesNotThrow(() -> {
            tariffRateCRUDService.deleteTariffRate(1L);
        });

        verify(tariffRateRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteTariffRate_NotExists_ThrowsException() {
        when(tariffRateRepository.existsById(999L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> {
            tariffRateCRUDService.deleteTariffRate(999L);
        });

        verify(tariffRateRepository, never()).deleteById(anyLong());
    }

    // Update Tests
    @Test
    void updateTariffRate_ValidUpdate_Success() {
        when(tariffRateRepository.findById(1L)).thenReturn(Optional.of(testTariffRate));
        when(countryRepository.existsByCountryCodeIgnoreCase(anyString())).thenReturn(true);
        when(tariffRateRepository.save(any(TariffRate.class))).thenReturn(testTariffRate);

        TariffRate result = tariffRateCRUDService.updateTariffRate(1L, validDTO);

        assertNotNull(result);
        verify(tariffRateRepository, times(1)).save(any(TariffRate.class));
    }

    @Test
    void updateTariffRate_NotFound_ThrowsException() {
        when(tariffRateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            tariffRateCRUDService.updateTariffRate(999L, validDTO);
        });
    }

    @Test
    void updateTariffRate_NullHsCode_SkipsUpdate() {
        validDTO.setHsCode(null);
        when(tariffRateRepository.findById(1L)).thenReturn(Optional.of(testTariffRate));
        when(countryRepository.existsByCountryCodeIgnoreCase(anyString())).thenReturn(true);
        when(tariffRateRepository.save(any(TariffRate.class))).thenReturn(testTariffRate);

        TariffRate result = tariffRateCRUDService.updateTariffRate(1L, validDTO);

        assertEquals("123456", result.getHsCode()); // Original value preserved
    }

    @Test
    void updateTariffRate_EmptyHsCode_SkipsUpdate() {
        validDTO.setHsCode("  ");
        when(tariffRateRepository.findById(1L)).thenReturn(Optional.of(testTariffRate));
        when(countryRepository.existsByCountryCodeIgnoreCase(anyString())).thenReturn(true);
        when(tariffRateRepository.save(any(TariffRate.class))).thenReturn(testTariffRate);

        TariffRate result = tariffRateCRUDService.updateTariffRate(1L, validDTO);

        assertEquals("123456", result.getHsCode()); // Original value preserved
    }

    @Test
    void updateTariffRate_InvalidImportingCountry_ThrowsException() {
        when(tariffRateRepository.findById(1L)).thenReturn(Optional.of(testTariffRate));
        when(countryRepository.existsByCountryCodeIgnoreCase("US")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            tariffRateCRUDService.updateTariffRate(1L, validDTO);
        });
    }

    @Test
    void updateTariffRate_InvalidExportingCountry_ThrowsException() {
        when(tariffRateRepository.findById(1L)).thenReturn(Optional.of(testTariffRate));
        when(countryRepository.existsByCountryCodeIgnoreCase("US")).thenReturn(true);
        when(countryRepository.existsByCountryCodeIgnoreCase("CN")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            tariffRateCRUDService.updateTariffRate(1L, validDTO);
        });
    }

    @Test
    void updateTariffRate_NegativeBaseRate_ThrowsException() {
        validDTO.setBaseRate(new BigDecimal("-10"));
        when(tariffRateRepository.findById(1L)).thenReturn(Optional.of(testTariffRate));
        when(countryRepository.existsByCountryCodeIgnoreCase(anyString())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            tariffRateCRUDService.updateTariffRate(1L, validDTO);
        });
    }

    @Test
    void updateTariffRate_NullBaseRate_SkipsUpdate() {
        validDTO.setBaseRate(null);
        when(tariffRateRepository.findById(1L)).thenReturn(Optional.of(testTariffRate));
        when(countryRepository.existsByCountryCodeIgnoreCase(anyString())).thenReturn(true);
        when(tariffRateRepository.save(any(TariffRate.class))).thenReturn(testTariffRate);

        TariffRate result = tariffRateCRUDService.updateTariffRate(1L, validDTO);

        assertNotNull(result);
    }

    @Test
    void updateTariffRate_UpdateYear_Success() {
        validDTO.setYear(2025);
        when(tariffRateRepository.findById(1L)).thenReturn(Optional.of(testTariffRate));
        when(countryRepository.existsByCountryCodeIgnoreCase(anyString())).thenReturn(true);
        when(tariffRateRepository.save(any(TariffRate.class))).thenAnswer(invocation -> {
            TariffRate saved = invocation.getArgument(0);
            assertEquals(2025, saved.getYear());
            return saved;
        });

        TariffRate result = tariffRateCRUDService.updateTariffRate(1L, validDTO);

        assertNotNull(result);
    }

    @Test
    void updateTariffRate_NullYear_SkipsYearUpdate() {
        validDTO.setYear(null);
        when(tariffRateRepository.findById(1L)).thenReturn(Optional.of(testTariffRate));
        when(countryRepository.existsByCountryCodeIgnoreCase(anyString())).thenReturn(true);
        when(tariffRateRepository.save(any(TariffRate.class))).thenReturn(testTariffRate);

        TariffRate result = tariffRateCRUDService.updateTariffRate(1L, validDTO);

        assertEquals(2024, result.getYear()); // Original year preserved
    }

    @Test
    void createTariffRate_ValidThreeCharacterCountryCode_Success() {
        validDTO.setImportingCountryCode("USA");
        validDTO.setExportingCountryCode("CHN");
        when(countryRepository.existsByCountryCodeIgnoreCase("USA")).thenReturn(true);
        when(countryRepository.existsByCountryCodeIgnoreCase("CHN")).thenReturn(true);
        when(tariffRateRepository.save(any(TariffRate.class))).thenReturn(testTariffRate);

        TariffRate result = tariffRateCRUDService.createTariffRate(validDTO);

        assertNotNull(result);
        verify(tariffRateRepository, times(1)).save(any(TariffRate.class));
    }

    @Test
    void createTariffRate_ZeroBaseRate_Success() {
        validDTO.setBaseRate(BigDecimal.ZERO);
        when(countryRepository.existsByCountryCodeIgnoreCase("US")).thenReturn(true);
        when(countryRepository.existsByCountryCodeIgnoreCase("CN")).thenReturn(true);
        when(tariffRateRepository.save(any(TariffRate.class))).thenReturn(testTariffRate);

        TariffRate result = tariffRateCRUDService.createTariffRate(validDTO);

        assertNotNull(result);
    }

    @Test
    void createTariffRate_NullBaseRate_Success() {
        validDTO.setBaseRate(null);
        when(countryRepository.existsByCountryCodeIgnoreCase("US")).thenReturn(true);
        when(countryRepository.existsByCountryCodeIgnoreCase("CN")).thenReturn(true);
        when(tariffRateRepository.save(any(TariffRate.class))).thenReturn(testTariffRate);

        TariffRate result = tariffRateCRUDService.createTariffRate(validDTO);

        assertNotNull(result);
    }

    @Test
    void updateTariffRate_NullImportingCountryCode_SkipsUpdate() {
        validDTO.setImportingCountryCode(null);
        when(tariffRateRepository.findById(1L)).thenReturn(Optional.of(testTariffRate));
        when(countryRepository.existsByCountryCodeIgnoreCase("CN")).thenReturn(true);
        when(tariffRateRepository.save(any(TariffRate.class))).thenReturn(testTariffRate);

        TariffRate result = tariffRateCRUDService.updateTariffRate(1L, validDTO);

        assertEquals("US", result.getImportingCountryCode()); // Original value preserved
    }

    @Test
    void updateTariffRate_EmptyImportingCountryCode_SkipsUpdate() {
        validDTO.setImportingCountryCode("  ");
        when(tariffRateRepository.findById(1L)).thenReturn(Optional.of(testTariffRate));
        when(countryRepository.existsByCountryCodeIgnoreCase("CN")).thenReturn(true);
        when(tariffRateRepository.save(any(TariffRate.class))).thenReturn(testTariffRate);

        TariffRate result = tariffRateCRUDService.updateTariffRate(1L, validDTO);

        assertEquals("US", result.getImportingCountryCode()); // Original value preserved
    }

    @Test
    void updateTariffRate_NullExportingCountryCode_SkipsUpdate() {
        validDTO.setExportingCountryCode(null);
        when(tariffRateRepository.findById(1L)).thenReturn(Optional.of(testTariffRate));
        when(countryRepository.existsByCountryCodeIgnoreCase("US")).thenReturn(true);
        when(tariffRateRepository.save(any(TariffRate.class))).thenReturn(testTariffRate);

        TariffRate result = tariffRateCRUDService.updateTariffRate(1L, validDTO);

        assertEquals("CN", result.getExportingCountryCode()); // Original value preserved
    }

    @Test
    void updateTariffRate_EmptyExportingCountryCode_SkipsUpdate() {
        validDTO.setExportingCountryCode("   ");
        when(tariffRateRepository.findById(1L)).thenReturn(Optional.of(testTariffRate));
        when(countryRepository.existsByCountryCodeIgnoreCase("US")).thenReturn(true);
        when(tariffRateRepository.save(any(TariffRate.class))).thenReturn(testTariffRate);

        TariffRate result = tariffRateCRUDService.updateTariffRate(1L, validDTO);

        assertEquals("CN", result.getExportingCountryCode()); // Original value preserved
    }

    @Test
    void getAllTariffRatesByDetails_MixedYearsWithRequestedYear_SortsCorrectly() {
        TariffRate rate2022 = new TariffRate();
        rate2022.setId(3L);
        rate2022.setYear(2022);

        TariffRate rate2023 = new TariffRate();
        rate2023.setId(2L);
        rate2023.setYear(2023);

        TariffRate rate2024 = new TariffRate();
        rate2024.setId(1L);
        rate2024.setYear(2024);

        TariffRate rateNoYear = new TariffRate();
        rateNoYear.setId(4L);
        rateNoYear.setYear(null);

        List<TariffRate> rates = Arrays.asList(rate2022, rateNoYear, rate2024, rate2023);
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "123456", "US", "CN")).thenReturn(rates);

        List<TariffRate> result = tariffRateCRUDService.getAllTariffRatesByDetails("123456", "US", "CN", 2023);

        assertEquals(4, result.size());
        assertEquals(2023, result.get(0).getYear()); // Requested year first
        assertEquals(2024, result.get(1).getYear()); // Then most recent
        assertEquals(2022, result.get(2).getYear()); // Then older
        assertNull(result.get(3).getYear()); // Null years last
    }

    @Test
    void getTariffRateByDetails_OnlyNullYears_ReturnsFirst() {
        TariffRate rateNoYear1 = new TariffRate();
        rateNoYear1.setId(1L);
        rateNoYear1.setYear(null);

        TariffRate rateNoYear2 = new TariffRate();
        rateNoYear2.setId(2L);
        rateNoYear2.setYear(null);

        TariffRate rateNoYear3 = new TariffRate();
        rateNoYear3.setId(3L);
        rateNoYear3.setYear(null);

        List<TariffRate> rates = Arrays.asList(rateNoYear1, rateNoYear2, rateNoYear3);
        when(tariffRateRepository.findByHsCodeAndImportingCountryCodeAndExportingCountryCode(
                "123456", "US", "CN")).thenReturn(rates);

        Optional<TariffRate> result = tariffRateCRUDService.getTariffRateByDetails("123456", "US", "CN");

        assertTrue(result.isPresent());
        assertNull(result.get().getYear());
    }
}
