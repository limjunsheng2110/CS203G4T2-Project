package com.cs203.tariffg4t2.controller;

import com.cs203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.cs203.tariffg4t2.dto.response.TariffCalculationResultDTO;
import com.cs203.tariffg4t2.service.tariffLogic.TariffCalculatorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "testuser", roles = {"USER"})
public class TariffControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TariffCalculatorService tariffCalculatorService;

    @Autowired
    private ObjectMapper objectMapper;

    private TariffCalculationRequestDTO validRequest;
    private TariffCalculationResultDTO mockResult;

    @BeforeEach
    void setUp() {
        // Setup valid request using the actual DTO
        validRequest = TariffCalculationRequestDTO.builder()
                .importingCountry("SG")
                .exportingCountry("US")
                .hsCode("010329")
                .productValue(new BigDecimal("10000.00"))
                .freight(new BigDecimal("200.00"))
                .insurance(new BigDecimal("50.00"))
                .heads(100)
                .weight(new BigDecimal("50.00"))
                .build();

        // Setup mock result using the actual DTO builder
        mockResult = TariffCalculationResultDTO.builder()
                .importingCountry("SG")
                .exportingCountry("US")
                .hsCode("010329")
                .productValue(new BigDecimal("10000.00"))
                .customsValue(new BigDecimal("10250.00"))
                .baseDuty(new BigDecimal("512.50"))
                .vatOrGst(new BigDecimal("756.38"))
                .shippingCost(new BigDecimal("200.00"))
                .tariffAmount(new BigDecimal("512.50"))
                .totalCost(new BigDecimal("11718.88"))
                .adValoremRate(new BigDecimal("0.05"))
                .calculationDate(LocalDateTime.now())
                .year(2024)
                .build();
    }

    @Test
    void testCalculateTariff_Success() throws Exception {
        // given
        when(tariffCalculatorService.calculate(any(TariffCalculationRequestDTO.class)))
                .thenReturn(mockResult);

        // when and then
        mockMvc.perform(post("/api/tariff/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importingCountry").value("SG"))
                .andExpect(jsonPath("$.exportingCountry").value("US"))
                .andExpect(jsonPath("$.hsCode").value("010329"))
                .andExpect(jsonPath("$.productValue").value(10000.00))
                .andExpect(jsonPath("$.customsValue").value(10250.00))
                .andExpect(jsonPath("$.baseDuty").value(512.50))
                .andExpect(jsonPath("$.tariffAmount").value(512.50))
                .andExpect(jsonPath("$.totalCost").value(11718.88))
                .andExpect(jsonPath("$.adValoremRate").value(0.05));
    }

    @Test
    void testCalculateTariff_WithMinimalFields() throws Exception {
        // given - only required fields
        TariffCalculationRequestDTO minimalRequest = TariffCalculationRequestDTO.builder()
                .importingCountry("SG")
                .exportingCountry("US")
                .hsCode("010329")
                .productValue(new BigDecimal("5000.00"))
                .heads(100)
                .weight(new BigDecimal("25.00"))
                .build();

        TariffCalculationResultDTO minimalResult = TariffCalculationResultDTO.builder()
                .importingCountry("SG")
                .exportingCountry("US")
                .hsCode("010329")
                .productValue(new BigDecimal("5000.00"))
                .customsValue(new BigDecimal("5000.00"))
                .baseDuty(new BigDecimal("250.00"))
                .vatOrGst(new BigDecimal("367.50"))
                .shippingCost(BigDecimal.ZERO)
                .tariffAmount(new BigDecimal("250.00"))
                .totalCost(new BigDecimal("5617.50"))
                .calculationDate(LocalDateTime.now())
                .build();

        when(tariffCalculatorService.calculate(any(TariffCalculationRequestDTO.class)))
                .thenReturn(minimalResult);

        // when and then
        mockMvc.perform(post("/api/tariff/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(minimalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(5617.50));
    }

    @Test
    void testCalculateTariff_InvalidCountryCode() throws Exception {
        // given
        TariffCalculationRequestDTO invalidRequest = TariffCalculationRequestDTO.builder()
                .importingCountry("INVALID")
                .exportingCountry("US")
                .hsCode("010329")
                .productValue(new BigDecimal("10000.00"))
                .build();

        when(tariffCalculatorService.calculate(any(TariffCalculationRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Validation errors: Invalid importing country code: INVALID"));

        // when and then
        mockMvc.perform(post("/api/tariff/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCalculateTariff_InvalidHsCode() throws Exception {
        // given
        TariffCalculationRequestDTO invalidRequest = TariffCalculationRequestDTO.builder()
                .importingCountry("SG")
                .exportingCountry("US")
                .hsCode("ABC")
                .productValue(new BigDecimal("10000.00"))
                .build();

        when(tariffCalculatorService.calculate(any(TariffCalculationRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Validation errors: HS code must be 6, 8, or 10 digits"));

        // when and then
        mockMvc.perform(post("/api/tariff/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCalculateTariff_NegativeProductValue() throws Exception {
        // given
        TariffCalculationRequestDTO invalidRequest = TariffCalculationRequestDTO.builder()
                .importingCountry("SG")
                .exportingCountry("US")
                .hsCode("010329")
                .productValue(new BigDecimal("-1000.00"))
                .build();

        when(tariffCalculatorService.calculate(any(TariffCalculationRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Validation errors: Product value must be positive"));

        // when and then
        mockMvc.perform(post("/api/tariff/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCalculateTariff_WithFreightAndInsurance() throws Exception {
        // given
        when(tariffCalculatorService.calculate(any(TariffCalculationRequestDTO.class)))
                .thenReturn(mockResult);

        // when and then
        mockMvc.perform(post("/api/tariff/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customsValue").value(10250.00)); // 10000 + 200 + 50
    }

    @Test
    void testCalculateTariff_MissingMandatoryFields() throws Exception {
        // given - missing productValue
        String invalidJson = "{\"importingCountry\":\"SG\",\"exportingCountry\":\"US\",\"hsCode\":\"010329\"}";

        when(tariffCalculatorService.calculate(any(TariffCalculationRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Missing required field: productValue"));

        // when and then
        mockMvc.perform(post("/api/tariff/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCalculateTariff_LargeProductValue() throws Exception {
        // given
        TariffCalculationRequestDTO largeValueRequest = TariffCalculationRequestDTO.builder()
                .importingCountry("SG")
                .exportingCountry("US")
                .hsCode("010329")
                .productValue(new BigDecimal("999999999.99"))
                .heads(100)
                .weight(new BigDecimal("1000.00"))
                .build();

        TariffCalculationResultDTO largeValueResult = TariffCalculationResultDTO.builder()
                .importingCountry("SG")
                .exportingCountry("US")
                .hsCode("010329")
                .productValue(new BigDecimal("999999999.99"))
                .customsValue(new BigDecimal("999999999.99"))
                .baseDuty(new BigDecimal("49999999.99"))
                .vatOrGst(new BigDecimal("73499999.99"))
                .shippingCost(BigDecimal.ZERO)
                .tariffAmount(new BigDecimal("49999999.99"))
                .totalCost(new BigDecimal("1123499999.97"))
                .calculationDate(LocalDateTime.now())
                .build();

        when(tariffCalculatorService.calculate(any(TariffCalculationRequestDTO.class)))
                .thenReturn(largeValueResult);

        // when and then
        mockMvc.perform(post("/api/tariff/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeValueRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").exists());
    }
}
