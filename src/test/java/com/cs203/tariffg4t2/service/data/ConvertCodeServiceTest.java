package com.cs203.tariffg4t2.service.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConvertCodeServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ConvertCodeService convertCodeService;

    @BeforeEach
    void setUp() {
        convertCodeService = new ConvertCodeService();
    }

    @Test
    void convertToISO3_CachedCountryCode_ReturnsFromCache() {
        String result = convertCodeService.convertToISO3("US");
        assertEquals("USA", result);
    }

    @Test
    void convertToISO3_AnotherCachedCountryCode_ReturnsFromCache() {
        String result = convertCodeService.convertToISO3("SG");
        assertEquals("SGP", result);
    }

    @Test
    void convertToISO3_CaseSensitivity_WorksWithLowercase() {
        String result = convertCodeService.convertToISO3("us");
        assertEquals("USA", result);
    }

    @Test
    void convertToISO3_CaseSensitivity_WorksWithMixedCase() {
        String result = convertCodeService.convertToISO3("Us");
        assertEquals("USA", result);
    }

    @Test
    void convertToISO3_NullInput_ReturnsNull() {
        String result = convertCodeService.convertToISO3(null);
        assertNull(result);
    }

    @Test
    void convertToISO3_EmptyString_ReturnsEmptyString() {
        String result = convertCodeService.convertToISO3("");
        assertEquals("", result);
    }

    @Test
    void convertToISO3_WhitespaceOnly_ReturnsOriginal() {
        String result = convertCodeService.convertToISO3("   ");
        assertEquals("   ", result);
    }

    @Test
    void convertToISO3_ThreeDigitCode_ReturnsAsIs() {
        String result = convertCodeService.convertToISO3("USA");
        assertEquals("USA", result);
    }

    @Test
    void convertToISO3_InvalidLength_ReturnsOriginal() {
        String result = convertCodeService.convertToISO3("USAA");
        assertEquals("USAA", result);
    }

    @Test
    void convertToISO3_SingleCharacter_ReturnsOriginal() {
        String result = convertCodeService.convertToISO3("U");
        assertEquals("U", result);
    }

    @Test
    void convertToISO3_AllCachedCountries_ReturnCorrectly() {
        assertEquals("CHN", convertCodeService.convertToISO3("CN"));
        assertEquals("JPN", convertCodeService.convertToISO3("JP"));
        assertEquals("KOR", convertCodeService.convertToISO3("KR"));
        assertEquals("THA", convertCodeService.convertToISO3("TH"));
        assertEquals("VNM", convertCodeService.convertToISO3("VN"));
        assertEquals("IDN", convertCodeService.convertToISO3("ID"));
        assertEquals("PHL", convertCodeService.convertToISO3("PH"));
        assertEquals("IND", convertCodeService.convertToISO3("IN"));
        assertEquals("AUS", convertCodeService.convertToISO3("AU"));
        assertEquals("NZL", convertCodeService.convertToISO3("NZ"));
        assertEquals("GBR", convertCodeService.convertToISO3("GB"));
        assertEquals("DEU", convertCodeService.convertToISO3("DE"));
        assertEquals("FRA", convertCodeService.convertToISO3("FR"));
        assertEquals("ITA", convertCodeService.convertToISO3("IT"));
        assertEquals("ESP", convertCodeService.convertToISO3("ES"));
        assertEquals("NLD", convertCodeService.convertToISO3("NL"));
        assertEquals("BEL", convertCodeService.convertToISO3("BE"));
        assertEquals("CHE", convertCodeService.convertToISO3("CH"));
        assertEquals("CAN", convertCodeService.convertToISO3("CA"));
        assertEquals("MEX", convertCodeService.convertToISO3("MX"));
        assertEquals("BRA", convertCodeService.convertToISO3("BR"));
        assertEquals("ARG", convertCodeService.convertToISO3("AR"));
        assertEquals("CHL", convertCodeService.convertToISO3("CL"));
    }

    @Test
    void clearCache_RemovesAllCachedEntries_ThenReinitializes() {
        // First, verify cache has entries
        assertTrue(convertCodeService.getCacheSize() > 0);
        int initialSize = convertCodeService.getCacheSize();

        // Clear cache
        convertCodeService.clearCache();

        // Cache should be re-initialized with common codes
        assertEquals(initialSize, convertCodeService.getCacheSize());
    }

    @Test
    void getCacheSize_ReturnsPositiveNumber() {
        int size = convertCodeService.getCacheSize();
        assertTrue(size > 0);
    }

    @Test
    void isCached_ForCachedCountry_ReturnsTrue() {
        assertTrue(convertCodeService.isCached("US"));
        assertTrue(convertCodeService.isCached("us"));
        assertTrue(convertCodeService.isCached("SG"));
    }

    @Test
    void isCached_ForUncachedCountry_ReturnsFalse() {
        assertFalse(convertCodeService.isCached("ZZ"));
    }

    @Test
    void convertToISO3_WithWhitespace_TrimsAndConverts() {
        String result = convertCodeService.convertToISO3("  US  ");
        assertEquals("USA", result);
    }
}

