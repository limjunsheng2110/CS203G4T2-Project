package com.cs203.tariffg4t2.controller;

import com.cs203.tariffg4t2.dto.request.PredictiveAnalysisRequest;
import com.cs203.tariffg4t2.dto.response.PredictiveAnalysisDiagnosticsResponse;
import com.cs203.tariffg4t2.dto.response.PredictiveAnalysisResponse;
import com.cs203.tariffg4t2.service.basic.PredictiveAnalysisService;
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

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "testuser", roles = {"USER"})
class PredictiveAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PredictiveAnalysisService predictiveAnalysisService;

    @Autowired
    private ObjectMapper objectMapper;

    private PredictiveAnalysisRequest validRequest;
    private PredictiveAnalysisResponse mockResponse;

    @BeforeEach
    void setUp() {
        validRequest = PredictiveAnalysisRequest.builder()
            .importingCountry("USA")
            .exportingCountry("China")
            .enableNewsAnalysis(true)
            .build();

        mockResponse = new PredictiveAnalysisResponse();
        mockResponse.setRecommendation("BUY");
        mockResponse.setConfidenceScore(0.85);
        mockResponse.setRationale("Positive sentiment and favorable exchange rates");
    }

    @Test
    void testGetPrediction_Success() throws Exception {
        when(predictiveAnalysisService.analyzePrediction(any(PredictiveAnalysisRequest.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(post("/api/predictive-analysis/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendation").value("BUY"))
            .andExpect(jsonPath("$.confidenceScore").value(0.85))
            .andExpect(jsonPath("$.rationale").value("Positive sentiment and favorable exchange rates"));

        verify(predictiveAnalysisService, times(1)).analyzePrediction(any(PredictiveAnalysisRequest.class));
    }

    @Test
    void testGetPrediction_WithoutNewsAnalysis() throws Exception {
        PredictiveAnalysisRequest request = PredictiveAnalysisRequest.builder()
            .importingCountry("USA")
            .exportingCountry("China")
            .enableNewsAnalysis(false)
            .build();

        mockResponse.setRecommendation("HOLD");
        when(predictiveAnalysisService.analyzePrediction(any(PredictiveAnalysisRequest.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(post("/api/predictive-analysis/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendation").value("HOLD"));
    }

    @Test
    void testGetPrediction_InvalidParameters() throws Exception {
        when(predictiveAnalysisService.analyzePrediction(any(PredictiveAnalysisRequest.class)))
            .thenThrow(new IllegalArgumentException("Invalid country code"));

        mockMvc.perform(post("/api/predictive-analysis/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid parameters"))
            .andExpect(jsonPath("$.details").value("Invalid country code"));
    }

    @Test
    void testGetPrediction_ServiceException() throws Exception {
        when(predictiveAnalysisService.analyzePrediction(any(PredictiveAnalysisRequest.class)))
            .thenThrow(new RuntimeException("Service unavailable"));

        mockMvc.perform(post("/api/predictive-analysis/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Prediction failed"))
            .andExpect(jsonPath("$.details").value("Service unavailable"));
    }

    @Test
    void testGetPredictionGet_Success() throws Exception {
        when(predictiveAnalysisService.analyzePrediction(any(PredictiveAnalysisRequest.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(get("/api/predictive-analysis/predict")
                .param("importingCountry", "USA")
                .param("exportingCountry", "China")
                .param("enableNewsAnalysis", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendation").value("BUY"))
            .andExpect(jsonPath("$.confidenceScore").value(0.85));
    }

    @Test
    void testGetPredictionGet_DefaultNewsAnalysis() throws Exception {
        when(predictiveAnalysisService.analyzePrediction(any(PredictiveAnalysisRequest.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(get("/api/predictive-analysis/predict")
                .param("importingCountry", "USA")
                .param("exportingCountry", "China"))
            .andExpect(status().isOk());
    }

    @Test
    void testGetPredictionGet_DisabledNewsAnalysis() throws Exception {
        when(predictiveAnalysisService.analyzePrediction(any(PredictiveAnalysisRequest.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(get("/api/predictive-analysis/predict")
                .param("importingCountry", "USA")
                .param("exportingCountry", "China")
                .param("enableNewsAnalysis", "false"))
            .andExpect(status().isOk());
    }

    @Test
    void testGetPredictionGet_InvalidParameters() throws Exception {
        when(predictiveAnalysisService.analyzePrediction(any(PredictiveAnalysisRequest.class)))
            .thenThrow(new IllegalArgumentException("Invalid country"));

        mockMvc.perform(get("/api/predictive-analysis/predict")
                .param("importingCountry", "INVALID")
                .param("exportingCountry", "INVALID"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/predictive-analysis/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("predictive-analysis-service"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.newsApiReference").value("https://newsapi.org/"));
    }

    @Test
    void testGetNewsAPIReference() throws Exception {
        mockMvc.perform(get("/api/predictive-analysis/news-api-reference"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.provider").value("NewsAPI.org"))
            .andExpect(jsonPath("$.url").value("https://newsapi.org/"))
            .andExpect(jsonPath("$.description").exists());
    }

    @Test
    void testDiagnostics_WithoutNewsApiTest() throws Exception {
        PredictiveAnalysisDiagnosticsResponse diagnostics = PredictiveAnalysisDiagnosticsResponse.builder()
            .newsApiKeyPresent(true)
            .exchangeRateAvailable(true)
            .build();

        when(predictiveAnalysisService.getDiagnostics(anyString(), anyString(), eq(false)))
            .thenReturn(diagnostics);

        mockMvc.perform(get("/api/predictive-analysis/debug/diagnostics")
                .param("importingCountry", "USA")
                .param("exportingCountry", "China")
                .param("testNewsApi", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.newsApiKeyPresent").value(true))
            .andExpect(jsonPath("$.exchangeRateAvailable").value(true));
    }

    @Test
    void testDiagnostics_WithNewsApiTest() throws Exception {
        PredictiveAnalysisDiagnosticsResponse diagnostics = PredictiveAnalysisDiagnosticsResponse.builder()
            .newsApiKeyPresent(true)
            .newsApiReachable(true)
            .build();

        when(predictiveAnalysisService.getDiagnostics(anyString(), anyString(), eq(true)))
            .thenReturn(diagnostics);

        mockMvc.perform(get("/api/predictive-analysis/debug/diagnostics")
                .param("importingCountry", "USA")
                .param("exportingCountry", "China")
                .param("testNewsApi", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.newsApiKeyPresent").value(true))
            .andExpect(jsonPath("$.newsApiReachable").value(true));
    }

    @Test
    void testDebugFetchNews_Success() throws Exception {
        List<Object> articles = new ArrayList<>();
        articles.add(new Object());
        articles.add(new Object());

        when(predictiveAnalysisService.debugFetchNews()).thenReturn((List) articles);

        mockMvc.perform(get("/api/predictive-analysis/debug/fetch-news"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.articlesFetched").value(2))
            .andExpect(jsonPath("$.message").value("Successfully fetched and stored news articles"));
    }

    @Test
    void testDebugFetchNews_Failure() throws Exception {
        when(predictiveAnalysisService.debugFetchNews())
            .thenThrow(new RuntimeException("API key not configured"));

        mockMvc.perform(get("/api/predictive-analysis/debug/fetch-news"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.error").value("API key not configured"))
            .andExpect(jsonPath("$.errorType").value("RuntimeException"));
    }

    @Test
    void testDebugFetchNews_WithCause() throws Exception {
        Exception cause = new IllegalStateException("News API unavailable");
        RuntimeException exception = new RuntimeException("Fetch failed", cause);

        when(predictiveAnalysisService.debugFetchNews()).thenThrow(exception);

        mockMvc.perform(get("/api/predictive-analysis/debug/fetch-news"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.details").value("News API unavailable"));
    }

    @Test
    void testGetPrediction_DifferentRecommendations() throws Exception {
        // Test WAIT recommendation
        mockResponse.setRecommendation("WAIT");
        when(predictiveAnalysisService.analyzePrediction(any(PredictiveAnalysisRequest.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(post("/api/predictive-analysis/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendation").value("WAIT"));

        // Test HOLD recommendation
        mockResponse.setRecommendation("HOLD");
        when(predictiveAnalysisService.analyzePrediction(any(PredictiveAnalysisRequest.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(post("/api/predictive-analysis/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendation").value("HOLD"));
    }

    @Test
    void testGetPrediction_LowConfidence() throws Exception {
        mockResponse.setConfidenceScore(0.3);
        when(predictiveAnalysisService.analyzePrediction(any(PredictiveAnalysisRequest.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(post("/api/predictive-analysis/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.confidenceScore").value(0.3));
    }

    @Test
    void testGetPrediction_HighConfidence() throws Exception {
        mockResponse.setConfidenceScore(0.95);
        when(predictiveAnalysisService.analyzePrediction(any(PredictiveAnalysisRequest.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(post("/api/predictive-analysis/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.confidenceScore").value(0.95));
    }

    @Test
    void testGetPrediction_DifferentCountryPairs() throws Exception {
        when(predictiveAnalysisService.analyzePrediction(any(PredictiveAnalysisRequest.class)))
            .thenReturn(mockResponse);

        // USA and UK
        PredictiveAnalysisRequest request1 = PredictiveAnalysisRequest.builder()
            .importingCountry("USA")
            .exportingCountry("UK")
            .enableNewsAnalysis(true)
            .build();

        mockMvc.perform(post("/api/predictive-analysis/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
            .andExpect(status().isOk());

        // Japan and Germany
        PredictiveAnalysisRequest request2 = PredictiveAnalysisRequest.builder()
            .importingCountry("Japan")
            .exportingCountry("Germany")
            .enableNewsAnalysis(false)
            .build();

        mockMvc.perform(post("/api/predictive-analysis/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
            .andExpect(status().isOk());
    }
}
