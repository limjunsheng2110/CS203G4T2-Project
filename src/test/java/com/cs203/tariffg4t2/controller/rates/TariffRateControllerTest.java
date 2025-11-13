package com.cs203.tariffg4t2.controller.rates;

import com.cs203.tariffg4t2.dto.basic.TariffRateDTO;
import com.cs203.tariffg4t2.model.basic.TariffRate;
import com.cs203.tariffg4t2.service.basic.TariffRateCRUDService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TariffRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TariffRateCRUDService tariffRateService;

    private TariffRate testTariffRate;
    private TariffRateDTO testTariffRateDTO;

    @BeforeEach
    void setUp() {
        testTariffRate = new TariffRate();
        testTariffRate.setId(1L);
        testTariffRate.setHsCode("1234.56");
        testTariffRate.setImportingCountryCode("US");
        testTariffRate.setExportingCountryCode("CN");
        testTariffRate.setAdValoremRate(new BigDecimal("5.5"));
        testTariffRate.setYear(2024);

        testTariffRateDTO = new TariffRateDTO();
        testTariffRateDTO.setHsCode("1234.56");
        testTariffRateDTO.setImportingCountryCode("US");
        testTariffRateDTO.setExportingCountryCode("CN");
        testTariffRateDTO.setBaseRate(new BigDecimal("5.5"));
        testTariffRateDTO.setYear(2024);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createTariffRate_Success() throws Exception {
        when(tariffRateService.createTariffRate(any(TariffRateDTO.class)))
                .thenReturn(testTariffRate);

        mockMvc.perform(post("/api/tariff-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testTariffRateDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.hsCode").value("1234.56"))
                .andExpect(jsonPath("$.importingCountryCode").value("US"));

        verify(tariffRateService, times(1)).createTariffRate(any(TariffRateDTO.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllTariffRates_Success() throws Exception {
        TariffRate rate2 = new TariffRate();
        rate2.setId(2L);
        rate2.setHsCode("7890.12");
        rate2.setImportingCountryCode("GB");
        rate2.setExportingCountryCode("DE");
        rate2.setAdValoremRate(new BigDecimal("3.5"));
        rate2.setYear(2024);

        List<TariffRate> tariffRates = Arrays.asList(testTariffRate, rate2);
        when(tariffRateService.getAllTariffRates()).thenReturn(tariffRates);

        mockMvc.perform(get("/api/tariff-rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(tariffRateService, times(1)).getAllTariffRates();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getTariffRateById_Found() throws Exception {
        when(tariffRateService.getTariffRateById(1L))
                .thenReturn(Optional.of(testTariffRate));

        mockMvc.perform(get("/api/tariff-rates/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.hsCode").value("1234.56"));

        verify(tariffRateService, times(1)).getTariffRateById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getTariffRateById_NotFound() throws Exception {
        when(tariffRateService.getTariffRateById(999L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/tariff-rates/999"))
                .andExpect(status().isNotFound());

        verify(tariffRateService, times(1)).getTariffRateById(999L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getTariffRateByDetails_Found() throws Exception {
        when(tariffRateService.getTariffRateByDetails("1234.56", "US", "CN"))
                .thenReturn(Optional.of(testTariffRate));

        mockMvc.perform(get("/api/tariff-rates/search")
                        .param("hsCode", "1234.56")
                        .param("importingCountryCode", "US")
                        .param("exportingCountryCode", "CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.hsCode").value("1234.56"));

        verify(tariffRateService, times(1))
                .getTariffRateByDetails("1234.56", "US", "CN");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getTariffRateByDetails_NotFound() throws Exception {
        when(tariffRateService.getTariffRateByDetails("9999.99", "XX", "YY"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/tariff-rates/search")
                        .param("hsCode", "9999.99")
                        .param("importingCountryCode", "XX")
                        .param("exportingCountryCode", "YY"))
                .andExpect(status().isNotFound());

        verify(tariffRateService, times(1))
                .getTariffRateByDetails("9999.99", "XX", "YY");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateTariffRate_Success() throws Exception {
        TariffRate updatedRate = new TariffRate();
        updatedRate.setId(1L);
        updatedRate.setHsCode("1234.56");
        updatedRate.setImportingCountryCode("US");
        updatedRate.setExportingCountryCode("CN");
        updatedRate.setAdValoremRate(new BigDecimal("7.5"));
        updatedRate.setYear(2024);

        when(tariffRateService.updateTariffRate(eq(1L), any(TariffRateDTO.class)))
                .thenReturn(updatedRate);

        TariffRateDTO updateDTO = new TariffRateDTO();
        updateDTO.setBaseRate(new BigDecimal("7.5"));

        mockMvc.perform(put("/api/tariff-rates/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adValoremRate").value(7.5));

        verify(tariffRateService, times(1))
                .updateTariffRate(eq(1L), any(TariffRateDTO.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateTariffRate_NotFound() throws Exception {
        when(tariffRateService.updateTariffRate(eq(999L), any(TariffRateDTO.class)))
                .thenThrow(new RuntimeException("Tariff rate not found"));

        mockMvc.perform(put("/api/tariff-rates/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testTariffRateDTO)))
                .andExpect(status().isNotFound());

        verify(tariffRateService, times(1))
                .updateTariffRate(eq(999L), any(TariffRateDTO.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteTariffRate_Success() throws Exception {
        doNothing().when(tariffRateService).deleteTariffRate(1L);

        mockMvc.perform(delete("/api/tariff-rates/1"))
                .andExpect(status().isNoContent());

        verify(tariffRateService, times(1)).deleteTariffRate(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteTariffRate_NotFound() throws Exception {
        doThrow(new RuntimeException("Tariff rate not found"))
                .when(tariffRateService).deleteTariffRate(999L);

        mockMvc.perform(delete("/api/tariff-rates/999"))
                .andExpect(status().isNotFound());

        verify(tariffRateService, times(1)).deleteTariffRate(999L);
    }
}
