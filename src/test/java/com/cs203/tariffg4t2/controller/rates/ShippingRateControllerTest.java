package com.cs203.tariffg4t2.controller.rates;

import com.cs203.tariffg4t2.dto.basic.ShippingRateDTO;
import com.cs203.tariffg4t2.service.basic.ShippingService;
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
class ShippingRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShippingService shippingService;

    private ShippingRateDTO testShippingRateDTO;

    @BeforeEach
    void setUp() {
        testShippingRateDTO = new ShippingRateDTO();
        testShippingRateDTO.setId(1L);
        testShippingRateDTO.setImportingCountryCode("US");
        testShippingRateDTO.setExportingCountryCode("CN");
        testShippingRateDTO.setAirRate(new BigDecimal("5.5"));
        testShippingRateDTO.setSeaRate(new BigDecimal("3.5"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllShippingRates_Success() throws Exception {
        ShippingRateDTO rate2 = new ShippingRateDTO();
        rate2.setId(2L);
        rate2.setImportingCountryCode("GB");
        rate2.setExportingCountryCode("DE");
        rate2.setAirRate(new BigDecimal("4.5"));
        rate2.setSeaRate(new BigDecimal("2.5"));

        List<ShippingRateDTO> rates = Arrays.asList(testShippingRateDTO, rate2);
        when(shippingService.getAllShippingRates()).thenReturn(rates);

        mockMvc.perform(get("/api/shipping-rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(shippingService, times(1)).getAllShippingRates();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getShippingRateById_Found() throws Exception {
        when(shippingService.getShippingRateById(1L))
                .thenReturn(Optional.of(testShippingRateDTO));

        mockMvc.perform(get("/api/shipping-rates/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.importingCountryCode").value("US"))
                .andExpect(jsonPath("$.airRate").value(5.5));

        verify(shippingService, times(1)).getShippingRateById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getShippingRateById_NotFound() throws Exception {
        when(shippingService.getShippingRateById(999L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/shipping-rates/999"))
                .andExpect(status().isNotFound());

        verify(shippingService, times(1)).getShippingRateById(999L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createShippingRate_Success() throws Exception {
        when(shippingService.createShippingRate(any(ShippingRateDTO.class)))
                .thenReturn(testShippingRateDTO);

        ShippingRateDTO newRate = new ShippingRateDTO();
        newRate.setImportingCountryCode("US");
        newRate.setExportingCountryCode("CN");
        newRate.setAirRate(new BigDecimal("5.5"));
        newRate.setSeaRate(new BigDecimal("3.5"));

        mockMvc.perform(post("/api/shipping-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.importingCountryCode").value("US"));

        verify(shippingService, times(1)).createShippingRate(any(ShippingRateDTO.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createShippingRate_BadRequest() throws Exception {
        when(shippingService.createShippingRate(any(ShippingRateDTO.class)))
                .thenThrow(new RuntimeException("Invalid data"));

        ShippingRateDTO newRate = new ShippingRateDTO();

        mockMvc.perform(post("/api/shipping-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRate)))
                .andExpect(status().isBadRequest());

        verify(shippingService, times(1)).createShippingRate(any(ShippingRateDTO.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateShippingRate_Success() throws Exception {
        ShippingRateDTO updatedRate = new ShippingRateDTO();
        updatedRate.setId(1L);
        updatedRate.setImportingCountryCode("US");
        updatedRate.setExportingCountryCode("CN");
        updatedRate.setAirRate(new BigDecimal("6.5"));
        updatedRate.setSeaRate(new BigDecimal("4.5"));

        when(shippingService.updateShippingRate(eq(1L), any(ShippingRateDTO.class)))
                .thenReturn(Optional.of(updatedRate));

        ShippingRateDTO updateDTO = new ShippingRateDTO();
        updateDTO.setAirRate(new BigDecimal("6.5"));
        updateDTO.setSeaRate(new BigDecimal("4.5"));

        mockMvc.perform(put("/api/shipping-rates/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.airRate").value(6.5));

        verify(shippingService, times(1))
                .updateShippingRate(eq(1L), any(ShippingRateDTO.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateShippingRate_NotFound() throws Exception {
        when(shippingService.updateShippingRate(eq(999L), any(ShippingRateDTO.class)))
                .thenReturn(Optional.empty());

        ShippingRateDTO updateDTO = new ShippingRateDTO();
        updateDTO.setAirRate(new BigDecimal("6.5"));

        mockMvc.perform(put("/api/shipping-rates/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());

        verify(shippingService, times(1))
                .updateShippingRate(eq(999L), any(ShippingRateDTO.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteShippingRate_Success() throws Exception {
        when(shippingService.deleteShippingRate(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/shipping-rates/1"))
                .andExpect(status().isOk());

        verify(shippingService, times(1)).deleteShippingRate(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteShippingRate_NotFound() throws Exception {
        when(shippingService.deleteShippingRate(999L)).thenReturn(false);

        mockMvc.perform(delete("/api/shipping-rates/999"))
                .andExpect(status().isNotFound());

        verify(shippingService, times(1)).deleteShippingRate(999L);
    }
}
