package com.cs203.tariffg4t2.service.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyCodeServiceTest {

    private CurrencyCodeService currencyCodeService;

    @BeforeEach
    void setUp() {
        currencyCodeService = new CurrencyCodeService();
    }

    @Test
    void getCurrencyCode_MajorCurrencies_ReturnsCorrectCode() {
        assertEquals("USD", currencyCodeService.getCurrencyCode("US"));
        assertEquals("USD", currencyCodeService.getCurrencyCode("USA"));
        assertEquals("GBP", currencyCodeService.getCurrencyCode("GB"));
        assertEquals("GBP", currencyCodeService.getCurrencyCode("GBR"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("EU"));
        assertEquals("JPY", currencyCodeService.getCurrencyCode("JP"));
        assertEquals("JPY", currencyCodeService.getCurrencyCode("JPN"));
        assertEquals("CNY", currencyCodeService.getCurrencyCode("CN"));
        assertEquals("CNY", currencyCodeService.getCurrencyCode("CHN"));
    }

    @Test
    void getCurrencyCode_SoutheastAsianCountries_ReturnsCorrectCode() {
        assertEquals("SGD", currencyCodeService.getCurrencyCode("SG"));
        assertEquals("SGD", currencyCodeService.getCurrencyCode("SGP"));
        assertEquals("MYR", currencyCodeService.getCurrencyCode("MY"));
        assertEquals("MYR", currencyCodeService.getCurrencyCode("MYS"));
        assertEquals("THB", currencyCodeService.getCurrencyCode("TH"));
        assertEquals("THB", currencyCodeService.getCurrencyCode("THA"));
        assertEquals("IDR", currencyCodeService.getCurrencyCode("ID"));
        assertEquals("IDR", currencyCodeService.getCurrencyCode("IDN"));
        assertEquals("PHP", currencyCodeService.getCurrencyCode("PH"));
        assertEquals("PHP", currencyCodeService.getCurrencyCode("PHL"));
        assertEquals("VND", currencyCodeService.getCurrencyCode("VN"));
        assertEquals("VND", currencyCodeService.getCurrencyCode("VNM"));
    }

    @Test
    void getCurrencyCode_EuropeanCountries_ReturnsEUR() {
        assertEquals("EUR", currencyCodeService.getCurrencyCode("DE"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("DEU"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("FR"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("FRA"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("IT"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("ITA"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("ES"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("ESP"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("NL"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("NLD"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("BE"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("BEL"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("AT"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("AUT"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("PT"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("PRT"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("IE"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("IRL"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("GR"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("GRC"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("FI"));
        assertEquals("EUR", currencyCodeService.getCurrencyCode("FIN"));
    }

    @Test
    void getCurrencyCode_OtherMajorEconomies_ReturnsCorrectCode() {
        assertEquals("CAD", currencyCodeService.getCurrencyCode("CA"));
        assertEquals("CAD", currencyCodeService.getCurrencyCode("CAN"));
        assertEquals("AUD", currencyCodeService.getCurrencyCode("AU"));
        assertEquals("AUD", currencyCodeService.getCurrencyCode("AUS"));
        assertEquals("NZD", currencyCodeService.getCurrencyCode("NZ"));
        assertEquals("NZD", currencyCodeService.getCurrencyCode("NZL"));
        assertEquals("CHF", currencyCodeService.getCurrencyCode("CH"));
        assertEquals("CHF", currencyCodeService.getCurrencyCode("CHE"));
        assertEquals("HKD", currencyCodeService.getCurrencyCode("HK"));
        assertEquals("HKD", currencyCodeService.getCurrencyCode("HKG"));
        assertEquals("INR", currencyCodeService.getCurrencyCode("IN"));
        assertEquals("INR", currencyCodeService.getCurrencyCode("IND"));
        assertEquals("KRW", currencyCodeService.getCurrencyCode("KR"));
        assertEquals("KRW", currencyCodeService.getCurrencyCode("KOR"));
    }

    @Test
    void getCurrencyCode_LatinAmericanCountries_ReturnsCorrectCode() {
        assertEquals("BRL", currencyCodeService.getCurrencyCode("BR"));
        assertEquals("BRL", currencyCodeService.getCurrencyCode("BRA"));
        assertEquals("MXN", currencyCodeService.getCurrencyCode("MX"));
        assertEquals("MXN", currencyCodeService.getCurrencyCode("MEX"));
        assertEquals("ARS", currencyCodeService.getCurrencyCode("AR"));
        assertEquals("ARS", currencyCodeService.getCurrencyCode("ARG"));
        assertEquals("CLP", currencyCodeService.getCurrencyCode("CL"));
        assertEquals("CLP", currencyCodeService.getCurrencyCode("CHL"));
        assertEquals("COP", currencyCodeService.getCurrencyCode("CO"));
        assertEquals("COP", currencyCodeService.getCurrencyCode("COL"));
        assertEquals("PEN", currencyCodeService.getCurrencyCode("PE"));
        assertEquals("PEN", currencyCodeService.getCurrencyCode("PER"));
    }

    @Test
    void getCurrencyCode_MiddleEastCountries_ReturnsCorrectCode() {
        assertEquals("AED", currencyCodeService.getCurrencyCode("AE"));
        assertEquals("AED", currencyCodeService.getCurrencyCode("ARE"));
        assertEquals("SAR", currencyCodeService.getCurrencyCode("SA"));
        assertEquals("SAR", currencyCodeService.getCurrencyCode("SAU"));
        assertEquals("ILS", currencyCodeService.getCurrencyCode("IL"));
        assertEquals("ILS", currencyCodeService.getCurrencyCode("ISR"));
    }

    @Test
    void getCurrencyCode_OtherCountries_ReturnsCorrectCode() {
        assertEquals("RUB", currencyCodeService.getCurrencyCode("RU"));
        assertEquals("RUB", currencyCodeService.getCurrencyCode("RUS"));
        assertEquals("ZAR", currencyCodeService.getCurrencyCode("ZA"));
        assertEquals("ZAR", currencyCodeService.getCurrencyCode("ZAF"));
        assertEquals("TRY", currencyCodeService.getCurrencyCode("TR"));
        assertEquals("TRY", currencyCodeService.getCurrencyCode("TUR"));
        assertEquals("PLN", currencyCodeService.getCurrencyCode("PL"));
        assertEquals("PLN", currencyCodeService.getCurrencyCode("POL"));
        assertEquals("SEK", currencyCodeService.getCurrencyCode("SE"));
        assertEquals("SEK", currencyCodeService.getCurrencyCode("SWE"));
        assertEquals("NOK", currencyCodeService.getCurrencyCode("NO"));
        assertEquals("NOK", currencyCodeService.getCurrencyCode("NOR"));
        assertEquals("DKK", currencyCodeService.getCurrencyCode("DK"));
        assertEquals("DKK", currencyCodeService.getCurrencyCode("DNK"));
        assertEquals("CZK", currencyCodeService.getCurrencyCode("CZ"));
        assertEquals("CZK", currencyCodeService.getCurrencyCode("CZE"));
    }

    @Test
    void getCurrencyCode_CaseInsensitive_ReturnsCorrectCode() {
        assertEquals("USD", currencyCodeService.getCurrencyCode("us"));
        assertEquals("USD", currencyCodeService.getCurrencyCode("Us"));
        assertEquals("GBP", currencyCodeService.getCurrencyCode("gb"));
        assertEquals("SGD", currencyCodeService.getCurrencyCode("sg"));
    }

    @Test
    void getCurrencyCode_NullInput_ReturnsNull() {
        assertNull(currencyCodeService.getCurrencyCode(null));
    }

    @Test
    void getCurrencyCode_EmptyString_ReturnsNull() {
        assertNull(currencyCodeService.getCurrencyCode(""));
    }

    @Test
    void getCurrencyCode_UnknownCountryCode_ReturnsNull() {
        assertNull(currencyCodeService.getCurrencyCode("ZZ"));
        assertNull(currencyCodeService.getCurrencyCode("XXX"));
    }

    @Test
    void hasCurrencyMapping_KnownCountries_ReturnsTrue() {
        assertTrue(currencyCodeService.hasCurrencyMapping("US"));
        assertTrue(currencyCodeService.hasCurrencyMapping("GB"));
        assertTrue(currencyCodeService.hasCurrencyMapping("SG"));
        assertTrue(currencyCodeService.hasCurrencyMapping("CN"));
        assertTrue(currencyCodeService.hasCurrencyMapping("us"));
    }

    @Test
    void hasCurrencyMapping_UnknownCountries_ReturnsFalse() {
        assertFalse(currencyCodeService.hasCurrencyMapping("ZZ"));
        assertFalse(currencyCodeService.hasCurrencyMapping("XXX"));
    }

    @Test
    void hasCurrencyMapping_NullInput_ReturnsFalse() {
        assertFalse(currencyCodeService.hasCurrencyMapping(null));
    }

    @Test
    void hasCurrencyMapping_EmptyString_ReturnsFalse() {
        assertFalse(currencyCodeService.hasCurrencyMapping(""));
    }

    @Test
    void hasCurrencyMapping_BothAlpha2AndAlpha3_ReturnsTrue() {
        assertTrue(currencyCodeService.hasCurrencyMapping("US"));
        assertTrue(currencyCodeService.hasCurrencyMapping("USA"));
        assertTrue(currencyCodeService.hasCurrencyMapping("CN"));
        assertTrue(currencyCodeService.hasCurrencyMapping("CHN"));
    }
}

