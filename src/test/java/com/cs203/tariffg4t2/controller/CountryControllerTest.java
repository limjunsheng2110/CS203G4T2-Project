package com.cs203.tariffg4t2.controller;


import com.cs203.tariffg4t2.model.basic.Country;
import com.cs203.tariffg4t2.service.basic.CountryService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
public class CountryControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CountryService countryService;

    private Country singaporeCountry;
    private Country usaCountry;
    private List<Country> countryList;

    @BeforeEach
    void setUp() {
        singaporeCountry = new Country();
        singaporeCountry.setCountryCode("SG");
        singaporeCountry.setCountryName("Singapore");
        singaporeCountry.setIso3Code("SGP");

        usaCountry = new Country();
        usaCountry.setCountryCode("US");
        usaCountry.setCountryName("United States");
        usaCountry.setIso3Code("USA");

        countryList = List.of(singaporeCountry, usaCountry);
    }

    @Test
    void testGetAllCountries() throws Exception {
        // given
        when(countryService.getAllCountries()).thenReturn(countryList);

        // when and then
        mockMvc.perform(get("/api/countries"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(countryList.size()))
               .andExpect(jsonPath("$[0].countryCode").value("SG"))
               .andExpect(jsonPath("$[0].countryName").value("Singapore"))
               .andExpect(jsonPath("$[0].iso3Code").value("SGP"))
               .andExpect(jsonPath("$[1].countryCode").value("US"))
               .andExpect(jsonPath("$[1].countryName").value("United States"))
               .andExpect(jsonPath("$[1].iso3Code").value("USA"));
    }

    @Test 
    void testGetAllCountries_EmptyList() throws Exception {
        // given
        when(countryService.getAllCountries()).thenReturn(Arrays.asList());

        // when and then
        mockMvc.perform(get("/api/countries"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(0));
    }

    @Test 
    void testGetCountryByCode_Success() throws Exception {
        // given
        when(countryService.getCountryByCode("SG")).thenReturn(singaporeCountry);

        // when and then
        mockMvc.perform(get("/api/countries/SG"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.countryCode").value("SG"))
               .andExpect(jsonPath("$.countryName").value("Singapore"))
               .andExpect(jsonPath("$.iso3Code").value("SGP"));
    }

    @Test
    void testGetCountryByCode_NotFound() throws Exception {
        // given
        when(countryService.getCountryByCode(anyString())).thenReturn(null);

        // when and then
        mockMvc.perform(get("/api/countries/XX"))
               .andExpect(status().isNotFound());
    }

    @Test
    void testGetCountryByCode_CaseInsensitive() throws Exception {
        // given
        when(countryService.getCountryByCode("sg")).thenReturn(singaporeCountry);

        // when and then
        mockMvc.perform(get("/api/countries/sg"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.countryCode").value("SG"))
               .andExpect(jsonPath("$.countryName").value("Singapore"))
               .andExpect(jsonPath("$.iso3Code").value("SGP"));
    }

    @Test
    void testGetCountryByInvalidCode_BadRequest() throws Exception {
        // given
        when(countryService.getCountryByCode("INVALID_CODE")).thenReturn(null);
        
        // when and then
        mockMvc.perform(get("/api/countries/INVALID_CODE"))
               .andExpect(status().is4xxClientError());
    }

    @Test
    void testGetCountryByEmptyCode() throws Exception {
        // given
        when(countryService.getCountryByCode(" ")).thenReturn(null);

        // when and then
        mockMvc.perform(get("/api/countries/ "))
               .andExpect(status().isNotFound());
    }
}
