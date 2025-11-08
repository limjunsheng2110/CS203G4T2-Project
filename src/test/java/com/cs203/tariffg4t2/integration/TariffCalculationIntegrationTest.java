package com.cs203.tariffg4t2.integration;

import com.cs203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TariffCalculationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private TariffCalculationRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new TariffCalculationRequestDTO();
        validRequest.setImportingCountry("SG");
        validRequest.setExportingCountry("US");
        validRequest.setHsCode("010329");
        validRequest.setProductValue(new BigDecimal("1000.00"));
        validRequest.setRooEligible(false);
        validRequest.setShippingMode("SEA");
        validRequest.setFreight(new BigDecimal("50.00"));
        validRequest.setInsurance(new BigDecimal("10.00"));
        validRequest.setWeight(new BigDecimal("100.00"));
        validRequest.setHeads(10);
    }

    // ========== SUCCESSFUL CALCULATIONS ==========

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_Success_MFN() throws Exception {
        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importingCountry").value("SG"))
                .andExpect(jsonPath("$.exportingCountry").value("US"))
                .andExpect(jsonPath("$.hsCode").value("010329"));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_Success_WithROO() throws Exception {
        // given
        validRequest.setRooEligible(true);

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_Success_AirShipping() throws Exception {
        // given
        validRequest.setShippingMode("AIR");

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shippingCost").exists());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_Success_LandShipping() throws Exception {
        // given
        validRequest.setImportingCountry("MY");
        validRequest.setExportingCountry("SG");
        validRequest.setShippingMode("LAND");

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_Success_HighValue() throws Exception {
        // given
        validRequest.setProductValue(new BigDecimal("100000.00"));
        validRequest.setFreight(new BigDecimal("5000.00"));
        validRequest.setInsurance(new BigDecimal("1000.00"));

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tariffAmount").exists());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_Success_DifferentHSCodes() throws Exception {
        // given - 6 digit HS code
        validRequest.setHsCode("010329");

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hsCode").value("010329"));

        // given - 10 digit HS code
        validRequest.setHsCode("0103290000");

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hsCode").value("0103290000"));
    }

    // ========== VALIDATION ERRORS ==========

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_MissingImportingCountry() throws Exception {
        // given
        validRequest.setImportingCountry(null);

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_MissingExportingCountry() throws Exception {
        // given
        validRequest.setExportingCountry(null);

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_MissingHsCode() throws Exception {
        // given
        validRequest.setHsCode(null);

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_InvalidHsCode_TooShort() throws Exception {
        // given
        validRequest.setHsCode("01032");

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_InvalidHsCode_TooLong() throws Exception {
        // given
        validRequest.setHsCode("01032900001");

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_InvalidCountry() throws Exception {
        // given
        validRequest.setImportingCountry("XX");

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_MultipleValidationErrors() throws Exception {
        // given
        validRequest.setImportingCountry(null);
        validRequest.setExportingCountry(null);
        validRequest.setHsCode(null);

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    // ========== DEFAULT VALUES ==========

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_DefaultProductValue() throws Exception {
        // given
        validRequest.setProductValue(null);

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_DefaultShippingMode() throws Exception {
        // given
        validRequest.setShippingMode(null);

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_DefaultFreightAndInsurance() throws Exception {
        // given
        validRequest.setFreight(null);
        validRequest.setInsurance(null);

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_DefaultWeightAndHeads() throws Exception {
        // given
        validRequest.setWeight(null);
        validRequest.setHeads(null);

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    // ========== AUTHENTICATION & AUTHORIZATION ==========

    @Test
    void testCalculateTariff_Unauthorized() throws Exception {
        // when & then - no authentication
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_AuthorizedUser() throws Exception {
        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void testCalculateTariff_AdminAccess() throws Exception {
        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    // ========== EDGE CASES ==========

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_ZeroProductValue() throws Exception {
        // given
        validRequest.setProductValue(BigDecimal.ZERO);

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_NegativeProductValue() throws Exception {
        // given
        validRequest.setProductValue(new BigDecimal("-100.00"));

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_VeryLargeWeight() throws Exception {
        // given
        validRequest.setWeight(new BigDecimal("99999.99"));

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_DecimalWeight() throws Exception {
        // given
        validRequest.setWeight(new BigDecimal("25.75"));

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_SameImportExportCountry() throws Exception {
        // given
        validRequest.setImportingCountry("SG");
        validRequest.setExportingCountry("SG");

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_CountryByName() throws Exception {
        // given
        validRequest.setImportingCountry("Singapore");
        validRequest.setExportingCountry("United States");

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importingCountry").value("SG"))
                .andExpect(jsonPath("$.exportingCountry").value("US"));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_HsCodeWithSpaces() throws Exception {
        // given
        validRequest.setHsCode("01 03 29");

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hsCode").value("010329"));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_CaseInsensitiveCountryCodes() throws Exception {
        // given
        validRequest.setImportingCountry("sg");
        validRequest.setExportingCountry("us");

        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importingCountry").value("SG"))
                .andExpect(jsonPath("$.exportingCountry").value("US"));
    }

    // ========== MALFORMED REQUESTS ==========

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_InvalidJson() throws Exception {
        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_EmptyRequestBody() throws Exception {
        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void testCalculateTariff_NullRequestBody() throws Exception {
        // when & then
        mockMvc.perform(post("/api/tariff/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest());
    }
}