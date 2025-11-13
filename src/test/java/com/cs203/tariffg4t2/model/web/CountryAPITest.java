package com.cs203.tariffg4t2.model.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CountryAPITest {

    @Test
    void testCountryAPIGettersAndSetters() {
        CountryAPI country = new CountryAPI();

        // Test code setter/getter
        country.setCode("US");
        assertEquals("US", country.getCode());
        assertEquals("US", country.getIso3Code());

        // Test name
        CountryAPI.CountryName name = new CountryAPI.CountryName();
        name.setCommon("United States");
        country.setName(name);
        assertEquals("United States", country.getName().getCommon());

        // Test region
        country.setRegion("Americas");
        assertEquals("Americas", country.getRegion());

        // Test currencies
        Object currencies = new Object();
        country.setCurrencies(currencies);
        assertNotNull(country.getCurrencies());
    }

    @Test
    void testCountryNameGettersAndSetters() {
        CountryAPI.CountryName name = new CountryAPI.CountryName();
        name.setCommon("United Kingdom");
        assertEquals("United Kingdom", name.getCommon());
    }

    @Test
    void testCountryAPIEqualsAndHashCode() {
        CountryAPI country1 = new CountryAPI();
        country1.setCode("GB");

        CountryAPI country2 = new CountryAPI();
        country2.setCode("GB");

        // Test equals (Lombok @Data generates equals/hashCode)
        assertEquals(country1, country2);
        assertEquals(country1.hashCode(), country2.hashCode());
    }

    @Test
    void testCountryAPIToString() {
        CountryAPI country = new CountryAPI();
        country.setCode("CA");
        country.setRegion("Americas");

        String toString = country.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("CA") || toString.contains("code"));
    }
}

