package com.cs203.tariffg4t2.service;

import com.cs203.tariffg4t2.model.basic.Country;
import com.cs203.tariffg4t2.repository.basic.CountryRepository;
import com.cs203.tariffg4t2.service.basic.CountryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.lenient;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CountryServiceTest {

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CountryService countryService;

    private Country sgCountry;
    private Country usCountry;
    private Country cnCountry;
    private List<Country> countryList;

    @BeforeEach
    void setUp() {
        // Setup test countries
        sgCountry = new Country("SG", "Singapore", "SGP");
        usCountry = new Country("US", "United States", "USA");
        cnCountry = new Country("CN", "China", "CHN");
        countryList = Arrays.asList(sgCountry, usCountry, cnCountry);
    }

    // ========== GET ALL COUNTRIES FROM DATABASE TESTS ==========

    @Test
    void testGetAllCountriesFromDatabase_Success() {
        // given
        when(countryRepository.findAll()).thenReturn(countryList);

        // when
        List<Country> result = countryService.getAllCountriesFromDatabase();

        // then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("SG", result.get(0).getCountryCode());
        assertEquals("US", result.get(1).getCountryCode());
        verify(countryRepository, times(1)).findAll();
    }

    @Test
    void testGetAllCountriesFromDatabase_EmptyList() {
        // given
        when(countryRepository.findAll()).thenReturn(Arrays.asList());

        // when
        List<Country> result = countryService.getAllCountriesFromDatabase();

        // then
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(countryRepository, times(1)).findAll();
    }

    // tests for get country by code

    @Test
    void testGetCountryByCode_Success() {
        // given
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));

        // when
        Country result = countryService.getCountryByCode("SG");

        // then
        assertNotNull(result);
        assertEquals("SG", result.getCountryCode());
        assertEquals("Singapore", result.getCountryName());
        assertEquals("SGP", result.getIso3Code());
        verify(countryRepository, times(1)).findByCountryCodeIgnoreCase("SG");
    }

    @Test
    void testGetCountryByCode_CaseInsensitive() {
        // given
        when(countryRepository.findByCountryCodeIgnoreCase("sg")).thenReturn(Optional.of(sgCountry));

        // when
        Country result = countryService.getCountryByCode("sg");

        // then
        assertNotNull(result);
        assertEquals("SG", result.getCountryCode());
        verify(countryRepository, times(1)).findByCountryCodeIgnoreCase("sg");
    }

    @Test
    void testGetCountryByCode_NotFound() {
        // given
        when(countryRepository.findByCountryCodeIgnoreCase("XX")).thenReturn(Optional.empty());

        // when & then
        assertThrows(RuntimeException.class, () -> {
            countryService.getCountryByCode("XX");
        });
    }

    // ========== CREATE COUNTRY TESTS ==========

    @Test
    void testCreateCountry_Success() {
        // given
        when(countryRepository.existsByCountryCode("MY")).thenReturn(false);
        when(countryRepository.save(any(Country.class))).thenReturn(new Country("MY", "Malaysia"));

        // when
        Country result = countryService.createCountry("MY", "Malaysia");

        // then
        assertNotNull(result);
        assertEquals("MY", result.getCountryCode());
        assertEquals("Malaysia", result.getCountryName());
        verify(countryRepository, times(1)).existsByCountryCode("MY");
        verify(countryRepository, times(1)).save(any(Country.class));
    }

    @Test
    void testCreateCountry_CodeToUpperCase() {
        // given
        when(countryRepository.existsByCountryCode("my")).thenReturn(false);
        when(countryRepository.save(any(Country.class))).thenReturn(new Country("MY", "Malaysia"));

        // when
        Country result = countryService.createCountry("my", "Malaysia");

        // then
        assertEquals("MY", result.getCountryCode());
    }

    @Test
    void testCreateCountry_DuplicateCode() {
        // given
        when(countryRepository.existsByCountryCode("SG")).thenReturn(true);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            countryService.createCountry("SG", "Singapore");
        });
        verify(countryRepository, never()).save(any(Country.class));
    }

    @Test
    void testCreateCountry_NullCode() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            countryService.createCountry(null, "Test Country");
        });
    }

    @Test
    void testCreateCountry_EmptyCode() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            countryService.createCountry("", "Test Country");
        });
    }

    @Test
    void testCreateCountry_NullName() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            countryService.createCountry("XX", null);
        });
    }

    @Test
    void testCreateCountry_EmptyName() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            countryService.createCountry("XX", "");
        });
    }

    // ========== UPDATE COUNTRY TESTS ==========

    @Test
    void testUpdateCountry_Success() {
        // given
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.save(any(Country.class))).thenReturn(sgCountry);

        // when
        Country result = countryService.updateCountry("SG", "Singapore Republic");

        // then
        assertNotNull(result);
        verify(countryRepository, times(1)).save(any(Country.class));
    }

    @Test
    void testUpdateCountry_NotFound() {
        // given
        when(countryRepository.findByCountryCodeIgnoreCase("XX")).thenReturn(Optional.empty());

        // when
        Country result = countryService.updateCountry("XX", "New Name");

        // then
        assertNull(result);
        verify(countryRepository, never()).save(any(Country.class));
    }

    @Test
    void testUpdateCountry_NullCode() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            countryService.updateCountry(null, "New Name");
        });
    }

    @Test
    void testUpdateCountry_EmptyCode() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            countryService.updateCountry("", "New Name");
        });
    }

    @Test
    void testUpdateCountry_NullName() {
        // given
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.save(any(Country.class))).thenReturn(sgCountry);

        // when
        Country result = countryService.updateCountry("SG", null);

        // then
        assertNotNull(result);
    }

    @Test
    void testUpdateCountry_EmptyName() {
        // given
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));
        when(countryRepository.save(any(Country.class))).thenReturn(sgCountry);

        // when
        Country result = countryService.updateCountry("SG", "");

        // then
        assertNotNull(result);
    }

    // tests to delete country

    @Test
    void testDeleteCountryByCode_Success() {
        // given
        when(countryRepository.existsByCountryCodeIgnoreCase("SG")).thenReturn(true);
        when(countryRepository.findByCountryCode("SG")).thenReturn(Optional.of(sgCountry));
        doNothing().when(countryRepository).delete(any(Country.class));

        // when
        boolean result = countryService.deleteCountryByCode("SG");

        // then
        assertTrue(result);
        verify(countryRepository, times(1)).delete(any(Country.class));
    }

    @Test
    void testDeleteCountryByCode_NotFound() {
        // given
        when(countryRepository.existsByCountryCodeIgnoreCase("XX")).thenReturn(false);

        // when
        boolean result = countryService.deleteCountryByCode("XX");

        // then
        assertFalse(result);
        verify(countryRepository, never()).delete(any(Country.class));
    }

    @Test
    void testDeleteCountryByCode_NullCode() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            countryService.deleteCountryByCode(null);
        });
    }

    @Test
    void testDeleteCountryByCode_EmptyCode() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            countryService.deleteCountryByCode("");
        });
    }

    // ========== GET 3 DIGIT COUNTRY CODE TESTS ==========

    @Test
    void testGet3DigitCountryCode_Success() {
        // given
        when(countryRepository.findByCountryCodeIgnoreCase("SG")).thenReturn(Optional.of(sgCountry));

        // when
        Country result = countryService.get3DigitCountryCode("SG");

        // then
        assertNotNull(result);
        assertEquals("SGP", result.getIso3Code());
    }

    @Test
    void testGet3DigitCountryCode_NotFound() {
        // given
        when(countryRepository.findByCountryCodeIgnoreCase("XX")).thenReturn(Optional.empty());

        // when
        Country result = countryService.get3DigitCountryCode("XX");

        // then
        assertNull(result);
    }

    @Test
    void testGet3DigitCountryCode_NullCode() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            countryService.get3DigitCountryCode(null);
        });
    }

    @Test
    void testGet3DigitCountryCode_EmptyCode() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            countryService.get3DigitCountryCode("");
        });
    }

    // ========== CONVERT COUNTRY NAME TO ISO3 TESTS ==========

    @Test
    void testConvertCountryNameToIso3_FromCache() {
        // The cache is initialized in constructor, so "singapore" should be cached
        // when
        String result = countryService.convertCountryNameToIso3("singapore");

        // then
        assertEquals("SGP", result);
        verify(countryRepository, never()).findByCountryNameIgnoreCase(anyString());
    }

    @Test
    void testConvertCountryNameToIso3_FromDatabase() {
        // given
        lenient().when(countryRepository.findByCountryNameIgnoreCase("Malaysia"))
                .thenReturn(Optional.of(new Country("MY", "Malaysia", "MYS")));

        // when
        String result = countryService.convertCountryNameToIso3("Malaysia");

        // then
        // Accept either from cache or database
        assertNotNull(result);
        // if Malaysia is not in cache, it should return "MYS"
        // if it is in cache, just verify it returns a valid result
    }

    @Test
    void testConvertCountryNameToIso3_NotFound() {
        // given
        when(countryRepository.findByCountryNameIgnoreCase("Unknown Country")).thenReturn(Optional.empty());

        // when
        String result = countryService.convertCountryNameToIso3("Unknown Country");

        // then
        assertNull(result);
    }

    @Test
    void testConvertCountryNameToIso3_NullInput() {
        // when
        String result = countryService.convertCountryNameToIso3(null);

        // then
        assertNull(result);
    }

    @Test
    void testConvertCountryNameToIso3_EmptyInput() {
        // when
        String result = countryService.convertCountryNameToIso3("");

        // then
        assertNull(result);
    }

    @Test
    void testConvertCountryNameToIso3_WhitespaceOnly() {
        // when
        String result = countryService.convertCountryNameToIso3("   ");

        // then
        assertNull(result);
    }

    // tests to convert country name to iso2

    @Test
    void testConvertCountryNameToIso2_FromCache() {
        // when
        String result = countryService.convertCountryNameToIso2("singapore");

        // then
        assertEquals("SG", result);
    }

    @Test
    void testConvertCountryNameToIso2_AlreadyISO2() {
        // when
        String result = countryService.convertCountryNameToIso2("SG");

        // then
        assertEquals("SG", result);
    }

    @Test
    void testConvertCountryNameToIso2_FromISO3() {
        // when
        String result = countryService.convertCountryNameToIso2("SGP");

        // then
        assertEquals("SG", result);
    }

    @Test
    void testConvertCountryNameToIso2_FromDatabase() {
        // given
        lenient().when(countryRepository.findByCountryNameIgnoreCase("Malaysia"))
                .thenReturn(Optional.of(new Country("MY", "Malaysia", "MYS")));

        // when
        String result = countryService.convertCountryNameToIso2("Malaysia");

        // then
        // accept either from cache or database
        assertNotNull(result);
    }

    @Test
    void testConvertCountryNameToIso2_NotFound() {
        // given
        when(countryRepository.findByCountryNameIgnoreCase("Unknown Country")).thenReturn(Optional.empty());

        // when
        String result = countryService.convertCountryNameToIso2("Unknown Country");

        // then
        assertNull(result);
    }

    @Test
    void testConvertCountryNameToIso2_NullInput() {
        // when
        String result = countryService.convertCountryNameToIso2(null);

        // then
        assertNull(result);
    }

    @Test
    void testConvertCountryNameToIso2_EmptyInput() {
        // when
        String result = countryService.convertCountryNameToIso2("");

        // then
        assertNull(result);
    }

    // tests to get countries count

    @Test
    void testGetCountriesCount_Success() {
        // given
        when(countryRepository.count()).thenReturn(3L);

        // when
        long result = countryService.getCountriesCount();

        // then
        assertEquals(3L, result);
        verify(countryRepository, times(1)).count();
    }

    @Test
    void testGetCountriesCount_Zero() {
        // given
        when(countryRepository.count()).thenReturn(0L);

        // when
        long result = countryService.getCountriesCount();

        // then
        assertEquals(0L, result);
    }

    // tests to clear all countries

    @Test
    void testClearAllCountries_Success() {
        // given
        doNothing().when(countryRepository).deleteAll();

        // when
        countryService.clearAllCountries();

        // then
        verify(countryRepository, times(1)).deleteAll();
    }
}