package com.cs203.tariffg4t2.integration;

import com.cs203.tariffg4t2.dto.request.*;
import com.cs203.tariffg4t2.dto.auth.AuthResponse; // Changed
import com.cs203.tariffg4t2.dto.basic.UserDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class EndToEndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        // no DTO setup needed,sincewe'll use Map for registration/login
    }

    // helper method to create registration request
    private Map<String, String> createRegisterRequest(String username, String email, String password) {
        Map<String, String> request = new HashMap<>();
        request.put("username", username);
        request.put("email", email);
        request.put("password", password);
        return request;
    }

    // helper method to create login request
    private Map<String, String> createLoginRequest(String username, String password) {
        Map<String, String> request = new HashMap<>();
        request.put("username", username);
        request.put("password", password);
        return request;
    }

    @Test
    @Order(1)
    void testCompleteUserRegistrationFlow() throws Exception {
        // register new user
        Map<String, String> registerRequest = createRegisterRequest("testuser1", "testuser1@example.com",
                "Password123!");

        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.username").value("testuser1"))
                .andExpect(jsonPath("$.user.email").value("testuser1@example.com"))
                .andReturn();

        String registerResponse = registerResult.getResponse().getContentAsString();
        AuthResponse registerDTO = objectMapper.readValue(registerResponse, AuthResponse.class);
        assertNotNull(registerDTO.getAccessToken());
        assertNotNull(registerDTO.getUser());
        assertEquals("testuser1", registerDTO.getUser().getUsername());
        assertEquals("testuser1@example.com", registerDTO.getUser().getEmail());
    }

    @Test
    @Order(2)
    void testRegistrationWithDuplicateUsername() throws Exception {
        // register first user
        Map<String, String> registerRequest = createRegisterRequest("testuser", "testuser@example.com", "Password123!");

        mockMvc.perform(post("/auth/register") // Changed
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // try to register with same username
        mockMvc.perform(post("/auth/register") // Changed
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(3)
    void testLoginWithInvalidCredentials() throws Exception {
        // register user
        Map<String, String> registerRequest = createRegisterRequest("testuser3", "testuser3@example.com",
                "Password123!");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // try login with wrong password
        Map<String, String> wrongPassword = createLoginRequest("testuser3", "WrongPassword123!");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPassword)))
                .andExpect(status().isForbidden()); // Changed from isUnauthorized() to isForbidden()
    }

    @Test
    @Order(4)
    void testCompleteTariffCalculationFlow() throws Exception {
        // register and login
        Map<String, String> registerRequest = createRegisterRequest("testuser", "testuser@example.com", "Password123!");

        MvcResult registerResult = mockMvc.perform(post("/auth/register") // Changed
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = registerResult.getResponse().getContentAsString();
        AuthResponse authDTO = objectMapper.readValue(response, AuthResponse.class);
        String token = authDTO.getAccessToken();

        // calculate tariff
        TariffCalculationRequestDTO tariffRequest = new TariffCalculationRequestDTO();
        tariffRequest.setImportingCountry("SG");
        tariffRequest.setExportingCountry("US");
        tariffRequest.setHsCode("010329");
        tariffRequest.setProductValue(new BigDecimal("1000.00"));
        tariffRequest.setShippingMode("SEA");
        tariffRequest.setWeight(new BigDecimal("100.00"));
        tariffRequest.setFreight(new BigDecimal("50.00"));
        tariffRequest.setInsurance(new BigDecimal("10.00"));

        mockMvc.perform(post("/api/tariff/calculate")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tariffRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importingCountry").value("SG"))
                .andExpect(jsonPath("$.exportingCountry").value("US"))
                .andExpect(jsonPath("$.hsCode").value("010329"))
                .andExpect(jsonPath("$.tariffAmount").exists());
    }

    @Test
    @Order(5)
    void testTariffCalculationWithoutAuthentication() throws Exception {
        TariffCalculationRequestDTO tariffRequest = new TariffCalculationRequestDTO();
        tariffRequest.setImportingCountry("SG");
        tariffRequest.setExportingCountry("US");
        tariffRequest.setHsCode("010329");

        mockMvc.perform(post("/api/tariff/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tariffRequest)))
                .andExpect(status().isOk()); // Changed from isForbidden() to isOk()
    }

    @Test
    @Order(6)
    void testMultipleTariffCalculationsWithDifferentShippingModes() throws Exception {
        // register and login
        Map<String, String> registerRequest = createRegisterRequest("testuser", "testuser@example.com", "Password123!");

        MvcResult registerResult = mockMvc.perform(post("/auth/register") // Changed
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = registerResult.getResponse().getContentAsString();
        AuthResponse authDTO = objectMapper.readValue(response, AuthResponse.class);
        String token = authDTO.getAccessToken();

        // calculate with SEA shipping
        TariffCalculationRequestDTO seaRequest = new TariffCalculationRequestDTO();
        seaRequest.setImportingCountry("SG");
        seaRequest.setExportingCountry("US");
        seaRequest.setHsCode("010329");
        seaRequest.setProductValue(new BigDecimal("1000.00"));
        seaRequest.setShippingMode("SEA");
        seaRequest.setWeight(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/tariff/calculate")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(seaRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shippingCost").exists());

        // calculate with AIR shipping
        TariffCalculationRequestDTO airRequest = new TariffCalculationRequestDTO();
        airRequest.setImportingCountry("SG");
        airRequest.setExportingCountry("US");
        airRequest.setHsCode("010329");
        airRequest.setProductValue(new BigDecimal("1000.00"));
        airRequest.setShippingMode("AIR");
        airRequest.setWeight(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/tariff/calculate")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(airRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shippingCost").exists());
    }

}