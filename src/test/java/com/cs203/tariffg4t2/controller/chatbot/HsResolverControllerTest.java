package com.cs203.tariffg4t2.controller.chatbot;

import com.cs203.tariffg4t2.dto.chatbot.HsCandidateDTO;
import com.cs203.tariffg4t2.dto.chatbot.HsResolveRequestDTO;
import com.cs203.tariffg4t2.dto.chatbot.HsResolveResponseDTO;
import com.cs203.tariffg4t2.service.chatbot.HsRateLimiter;
import com.cs203.tariffg4t2.service.chatbot.HsResolverService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "testuser", roles = {"USER"})
public class HsResolverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HsResolverService hsResolverService;

    @MockitoBean
    private HsRateLimiter hsRateLimiter;

    private HsResolveRequestDTO validRequest;
    private HsResolveResponseDTO mockResponse;

    @BeforeEach
    void setUp() {
        // Setup valid request
        validRequest = new HsResolveRequestDTO();
        validRequest.setProductName("chicken");
        validRequest.setDescription("live chickens for farming purposes");
        validRequest.setSessionId("test-session-123");
        validRequest.setQueryId("test-query-456");
        validRequest.setConsentLogging(true);

        // Setup mock response with candidates
        HsCandidateDTO candidate1 = HsCandidateDTO.builder()
                .hsCode("010511")
                .confidence(0.85)
                .rationale("Live poultry, chickens")
                .source("PRODUCT_DATABASE")
                .attributesUsed(Arrays.asList("chicken", "live"))
                .build();

        HsCandidateDTO candidate2 = HsCandidateDTO.builder()
                .hsCode("020714")
                .confidence(0.65)
                .rationale("Cuts and edible offal of fowls, fresh or chilled")
                .source("PRODUCT_DATABASE")
                .attributesUsed(Arrays.asList("chicken"))
                .build();

        mockResponse = HsResolveResponseDTO.builder()
                .queryId("test-query-456")
                .sessionId("test-session-123")
                .candidates(Arrays.asList(candidate1, candidate2))
                .disambiguationQuestions(List.of())
                .build();

        // Default: allow rate limiting
        when(hsRateLimiter.tryConsume(anyString())).thenReturn(true);
    }

    @Test
    void testResolveHsCode_Success() throws Exception {
        // given
        when(hsResolverService.resolveHsCode(any(HsResolveRequestDTO.class)))
                .thenReturn(mockResponse);

        // when and then
        mockMvc.perform(post("/api/hs/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryId").value("test-query-456"))
                .andExpect(jsonPath("$.sessionId").value("test-session-123"))
                .andExpect(jsonPath("$.candidates").isArray())
                .andExpect(jsonPath("$.candidates.length()").value(2))
                .andExpect(jsonPath("$.candidates[0].hsCode").value("010511"))
                .andExpect(jsonPath("$.candidates[0].confidence").value(0.85))
                .andExpect(jsonPath("$.candidates[1].hsCode").value("020714"));
    }

    @Test
    void testResolveHsCode_RateLimitExceeded() throws Exception {
        // given
        when(hsRateLimiter.tryConsume(anyString())).thenReturn(false);

        // when and then
        mockMvc.perform(post("/api/hs/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void testResolveHsCode_ValidationError_DescriptionTooShort() throws Exception {
        // given
        validRequest.setDescription("short"); // Less than 10 characters

        // when and then
        mockMvc.perform(post("/api/hs/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testResolveHsCode_ValidationError_DescriptionTooLong() throws Exception {
        // given
        String longDescription = "a".repeat(2001); // More than 2000 characters
        validRequest.setDescription(longDescription);

        // when and then
        mockMvc.perform(post("/api/hs/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testResolveHsCode_WithForwardedForHeader() throws Exception {
        // given
        when(hsResolverService.resolveHsCode(any(HsResolveRequestDTO.class)))
                .thenReturn(mockResponse);

        // when and then
        mockMvc.perform(post("/api/hs/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "192.168.1.1, 10.0.0.1")
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("test-session-123"));
    }

    @Test
    void testResolveHsCode_EmptyCandidates() throws Exception {
        // given
        HsResolveResponseDTO emptyResponse = HsResolveResponseDTO.builder()
                .queryId("test-query-456")
                .sessionId("test-session-123")
                .candidates(List.of())
                .disambiguationQuestions(List.of())
                .build();

        when(hsResolverService.resolveHsCode(any(HsResolveRequestDTO.class)))
                .thenReturn(emptyResponse);

        // when and then
        mockMvc.perform(post("/api/hs/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates").isArray())
                .andExpect(jsonPath("$.candidates.length()").value(0));
    }

    @Test
    void testResolveHsCode_MinimumValidDescription() throws Exception {
        // given
        validRequest.setDescription("1234567890"); // Exactly 10 characters
        when(hsResolverService.resolveHsCode(any(HsResolveRequestDTO.class)))
                .thenReturn(mockResponse);

        // when and then
        mockMvc.perform(post("/api/hs/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void testResolveHsCode_WithoutSessionId() throws Exception {
        // given
        validRequest.setSessionId(null); // No session ID provided
        when(hsResolverService.resolveHsCode(any(HsResolveRequestDTO.class)))
                .thenReturn(mockResponse);

        // when and then
        mockMvc.perform(post("/api/hs/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }
}
