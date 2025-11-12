package com.cs203.tariffg4t2.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cs203.tariffg4t2.dto.chatbot.HsResolveRequestDTO;
import com.cs203.tariffg4t2.model.chatbot.HsChatSessionLog;
import com.cs203.tariffg4t2.model.chatbot.HsReference;
import com.cs203.tariffg4t2.repository.chatbot.HsChatSessionLogRepository;
import com.cs203.tariffg4t2.repository.chatbot.HsReferenceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:hsresolver;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=password",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@SuppressWarnings("DataFlowIssue")
class HsResolverIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HsReferenceRepository hsReferenceRepository;

    @Autowired
    private HsChatSessionLogRepository hsChatSessionLogRepository;

    @BeforeEach
    @SuppressWarnings("DataFlowIssue")
    void setUp() {
        hsReferenceRepository.deleteAll();
        hsChatSessionLogRepository.deleteAll();

        HsReference reference = new HsReference();
        reference.setHsCode("8516.10.00");
        reference.setDescription("Electric hair dryers with multiple heat settings");
        reference.setKeywords("hair,dryer,blow,dry");
        hsReferenceRepository.save(reference);
    }

    @Test
    void resolveHsCode_returnsCandidateAndPersistsLog() throws Exception {
        HsResolveRequestDTO request = HsResolveRequestDTO.builder()
                .sessionId("session-integration")
                .productName("Hair Dryer")
                .description("Compact electric hair dryer with ionic technology and multiple heat settings.")
                .attributes(java.util.List.of("electric", "hair care"))
                .consentLogging(true)
                .build();

        String payload = objectMapper.writeValueAsString(request);
        assertNotNull(payload);

        mockMvc.perform(post("/api/hs/resolve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-integration"))
                .andExpect(jsonPath("$.candidates[0].hsCode").value("8516.10.00"))
                .andExpect(jsonPath("$.candidates[0].rationale").value("Electric hair dryers with multiple heat settings"))
                .andExpect(jsonPath("$.notice.consentGranted").value(true))
                .andExpect(jsonPath("$.fallback").isNotEmpty());

        assertTrue(hsChatSessionLogRepository.findById("session-integration")
                .map(HsChatSessionLog::getRequestCount)
                .filter(count -> count >= 1)
                .isPresent());
    }

    @Test
    void resolveHsCode_enforcesSessionRateLimit() throws Exception {
        HsResolveRequestDTO request = HsResolveRequestDTO.builder()
                .sessionId("session-limit")
                .productName("Hair Dryer")
                .description("Compact electric hair dryer with ionic technology.")
                .attributes(java.util.List.of("electric"))
                .consentLogging(false)
                .build();

        String limitPayload = objectMapper.writeValueAsString(request);
        assertNotNull(limitPayload);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/hs/resolve")
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .content(limitPayload))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/hs/resolve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(limitPayload))
                .andExpect(status().isTooManyRequests());
    }
}

