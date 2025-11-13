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
                .productValue(new BigDecimal("1000.00"))
                .weight(new BigDecimal("10.00"))
                .build();

        TariffCalculationResultDTO minimalResult = TariffCalculationResultDTO.builder()
                .importingCountry("SG")
                .exportingCountry("US")
                .hsCode("010329")
                .productValue(new BigDecimal("1000.00"))
                .customsValue(new BigDecimal("1000.00"))
                .baseDuty(new BigDecimal("50.00"))
                .vatOrGst(new BigDecimal("73.50"))
                .shippingCost(new BigDecimal("20.00"))
                .tariffAmount(new BigDecimal("50.00"))
                .totalCost(new BigDecimal("1143.50"))
                .calculationDate(LocalDateTime.now())
                .build();

        when(tariffCalculatorService.calculate(any(TariffCalculationRequestDTO.class)))
                .thenReturn(minimalResult);

        // when and then
        mockMvc.perform(post("/api/tariff/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(minimalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importingCountry").value("SG"))
                .andExpect(jsonPath("$.totalCost").value(1143.50));
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

    @Test
    void testCalculateTariff_WithInvalidData_ThrowsException() throws Exception {
        // given
        when(tariffCalculatorService.calculate(any(TariffCalculationRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Validation errors: Importing country is required"));

        // when and then
        mockMvc.perform(post("/api/tariff/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Validation errors: Importing country is required"));
    }

    @Test
    void testCalculateTariff_WithNullValues() throws Exception {
        // given - request with null optional fields
        TariffCalculationRequestDTO requestWithNulls = TariffCalculationRequestDTO.builder()
                .importingCountry("SG")
                .exportingCountry("US")
                .hsCode("010329")
                .productValue(new BigDecimal("1000.00"))
                .weight(new BigDecimal("10.00"))
                .freight(null)
                .insurance(null)
                .heads(null)
                .build();

        when(tariffCalculatorService.calculate(any(TariffCalculationRequestDTO.class)))
                .thenReturn(mockResult);

        // when and then
        mockMvc.perform(post("/api/tariff/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithNulls)))
                .andExpect(status().isOk());
    }

    @Test
    void testCalculateTariffGet_Success() throws Exception {
        // given
        when(tariffCalculatorService.calculate(any(TariffCalculationRequestDTO.class)))
                .thenReturn(mockResult);

        // when and then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/tariff/calculate")
                .param("importingCountry", "SG")
                .param("exportingCountry", "US")
                .param("hsCode", "010329")
                .param("productValue", "10000.00")
                .param("weight", "50.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importingCountry").value("SG"))
                .andExpect(jsonPath("$.exportingCountry").value("US"))
                .andExpect(jsonPath("$.hsCode").value("010329"));
    }

    @Test
    void testCalculateTariffGet_WithInvalidData() throws Exception {
        // given
        when(tariffCalculatorService.calculate(any(TariffCalculationRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Invalid HS code format"));

        // when and then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/tariff/calculate")
                .param("importingCountry", "SG")
                .param("exportingCountry", "US")
                .param("hsCode", "ABC")
                .param("productValue", "10000.00")
                .param("weight", "50.00"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid HS code format"));
    }

    @Test
    void testCalculateTariff_WithLargeValues() throws Exception {
        // given - very large values
        TariffCalculationRequestDTO largeRequest = TariffCalculationRequestDTO.builder()
                .importingCountry("SG")
                .exportingCountry("US")
                .hsCode("010329")
                .productValue(new BigDecimal("1000000.00"))
                .weight(new BigDecimal("5000.00"))
                .freight(new BigDecimal("10000.00"))
                .insurance(new BigDecimal("5000.00"))
                .build();

        TariffCalculationResultDTO largeResult = TariffCalculationResultDTO.builder()
                .importingCountry("SG")
                .exportingCountry("US")
                .hsCode("010329")
                .productValue(new BigDecimal("1000000.00"))
                .customsValue(new BigDecimal("1015000.00"))
                .totalCost(new BigDecimal("1200000.00"))
                .calculationDate(LocalDateTime.now())
                .build();

        when(tariffCalculatorService.calculate(any(TariffCalculationRequestDTO.class)))
                .thenReturn(largeResult);

        // when and then
        mockMvc.perform(post("/api/tariff/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(1200000.00));
    }

    @Test
    void testCalculateTariff_WithDecimalPrecision() throws Exception {
        // given - values with high decimal precision
        TariffCalculationRequestDTO precisionRequest = TariffCalculationRequestDTO.builder()
                .importingCountry("SG")
                .exportingCountry("US")
                .hsCode("010329")
                .productValue(new BigDecimal("1234.56"))
                .weight(new BigDecimal("12.345"))
                .build();

        when(tariffCalculatorService.calculate(any(TariffCalculationRequestDTO.class)))
                .thenReturn(mockResult);

        // when and then
        mockMvc.perform(post("/api/tariff/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(precisionRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void testCalculateTariff_WithDifferentCountryPairs() throws Exception {
        // given - different country combination
        TariffCalculationRequestDTO differentCountries = TariffCalculationRequestDTO.builder()
                .importingCountry("US")
                .exportingCountry("CN")
                .hsCode("123456")
                .productValue(new BigDecimal("5000.00"))
                .weight(new BigDecimal("25.00"))
                .build();

        TariffCalculationResultDTO differentResult = TariffCalculationResultDTO.builder()
                .importingCountry("US")
                .exportingCountry("CN")
                .hsCode("123456")
                .totalCost(new BigDecimal("6000.00"))
                .calculationDate(LocalDateTime.now())
                .build();

        when(tariffCalculatorService.calculate(any(TariffCalculationRequestDTO.class)))
                .thenReturn(differentResult);

        // when and then
        mockMvc.perform(post("/api/tariff/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(differentCountries)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importingCountry").value("US"))
                .andExpect(jsonPath("$.exportingCountry").value("CN"));
    }

    @Test
    void testCalculateTariff_WithYearSpecified() throws Exception {
        // given - request with specific year
        TariffCalculationRequestDTO yearRequest = TariffCalculationRequestDTO.builder()
                .importingCountry("SG")
                .exportingCountry("US")
                .hsCode("010329")
                .productValue(new BigDecimal("1000.00"))
                .weight(new BigDecimal("10.00"))
                .year(2024)
                .build();

        TariffCalculationResultDTO yearResult = TariffCalculationResultDTO.builder()
                .importingCountry("SG")
                .exportingCountry("US")
                .hsCode("010329")
                .totalCost(new BigDecimal("1200.00"))
                .year(2024)
                .calculationDate(LocalDateTime.now())
                .build();

        when(tariffCalculatorService.calculate(any(TariffCalculationRequestDTO.class)))
                .thenReturn(yearResult);

        // when and then
        mockMvc.perform(post("/api/tariff/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(yearRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2024));
    }

    @Test
    void testCalculateTariff_WithShippingMode() throws Exception {
        // given - request with specific shipping mode
        TariffCalculationRequestDTO shippingRequest = TariffCalculationRequestDTO.builder()
                .importingCountry("SG")
                .exportingCountry("US")
                .hsCode("010329")
                .productValue(new BigDecimal("1000.00"))
                .weight(new BigDecimal("10.00"))
                .shippingMode("AIR")
                .build();

        when(tariffCalculatorService.calculate(any(TariffCalculationRequestDTO.class)))
                .thenReturn(mockResult);

        // when and then
        mockMvc.perform(post("/api/tariff/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(shippingRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void testCalculateTariff_ExceptionHandler() throws Exception {
        // given
        when(tariffCalculatorService.calculate(any(TariffCalculationRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Multiple validation errors"));

        // when and then
        mockMvc.perform(post("/api/tariff/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(content().string("Multiple validation errors"));
    }
}
