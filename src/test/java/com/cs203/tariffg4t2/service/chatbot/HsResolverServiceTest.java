package com.cs203.tariffg4t2.service.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cs203.tariffg4t2.dto.chatbot.HsResolveRequestDTO;
import com.cs203.tariffg4t2.dto.chatbot.HsResolveResponseDTO;
import com.cs203.tariffg4t2.dto.chatbot.PreviousAnswerDTO;
import com.cs203.tariffg4t2.model.chatbot.HsReference;
import com.cs203.tariffg4t2.repository.chatbot.HsReferenceRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("DataFlowIssue")
class HsResolverServiceTest {

    @Mock
    private HsReferenceRepository hsReferenceRepository;

    @Mock
    private HsChatSessionLogService hsChatSessionLogService;

    @InjectMocks
    private HsResolverService hsResolverService;

    private HsReference toothbrushReference;

    @BeforeEach
    void setUp() {
        toothbrushReference = HsReference.builder()
                .hsCode("9603.21.00")
                .description("Toothbrushes, including dental plate brushes")
                .keywords("toothbrush,dental,oral care")
                .build();
    }

    @Test
    void resolveHsCode_returnsCandidatesFromReferenceMatches() {
        HsResolveRequestDTO request = HsResolveRequestDTO.builder()
                .sessionId("session-123")
                .queryId(UUID.randomUUID().toString())
                .productName("Electric Toothbrush")
                .description("Rechargeable electric toothbrush with multiple brushing modes.")
                .attributes(List.of("battery powered", "oral care"))
                .previousAnswers(List.of())
                .consentLogging(true)
                .build();

        when(hsReferenceRepository.searchByToken("electric")).thenReturn(List.of(toothbrushReference));
        when(hsReferenceRepository.searchByToken("toothbrush")).thenReturn(List.of(toothbrushReference));
        when(hsReferenceRepository.searchByToken("rechargeable")).thenReturn(List.of(toothbrushReference));
        when(hsReferenceRepository.searchByToken("battery")).thenReturn(List.of(toothbrushReference));
        when(hsReferenceRepository.searchByToken("powered")).thenReturn(List.of(toothbrushReference));
        when(hsReferenceRepository.searchByToken("oral")).thenReturn(List.of(toothbrushReference));
        when(hsReferenceRepository.searchByToken("care")).thenReturn(List.of(toothbrushReference));

        HsResolveResponseDTO response = hsResolverService.resolveHsCode(request);

        assertNotNull(response);
        assertEquals("session-123", response.getSessionId());
        assertNotNull(response.getCandidates());
        assertFalse(response.getCandidates().isEmpty());
        assertEquals("9603.21.00", response.getCandidates().get(0).getHsCode());
        assertEquals("REFERENCE", response.getCandidates().get(0).getSource());
        assertTrue(response.getCandidates().get(0).getConfidence() > 0.5);

        ArgumentCaptor<Boolean> consentCaptor = ArgumentCaptor.forClass(Boolean.class);
        //noinspection DataFlowIssue
        verify(hsChatSessionLogService, times(1))
                .recordInteraction(anyString(), consentCaptor.capture(),
                        any(HsResolveRequestDTO.class), any(HsResolveResponseDTO.class));
        Boolean capturedConsent = consentCaptor.getValue();
        assertNotNull(capturedConsent);
        assertTrue(capturedConsent);
    }

    @Test
    void resolveHsCode_usesFallbackWhenNoMatches() {
        HsResolveRequestDTO request = HsResolveRequestDTO.builder()
                .productName("Unknown gadget")
                .description("Some description that does not exist in reference")
                .attributes(List.of("attribute"))
                .consentLogging(false)
                .build();

        when(hsReferenceRepository.searchByToken(any())).thenReturn(List.of());

        HsResolveResponseDTO response = hsResolverService.resolveHsCode(request);

        assertNotNull(response);
        assertNotNull(response.getCandidates());
        assertEquals("0000.00.00", response.getCandidates().get(0).getHsCode());
        assertEquals("RULE", response.getCandidates().get(0).getSource());

        verifyNoInteractions(hsChatSessionLogService);
    }

    @Test
    void resolveHsCode_sanitizesRequestBeforeLogging() {
        HsResolveRequestDTO request = HsResolveRequestDTO.builder()
                .sessionId("session-sanitize")
                .productName("  <b>Laser</b> Cutter  ")
                .description("Line1\nLine2\t<script>alert('x')</script>")
                .attributes(List.of("  industrial  ", "laser  ", "<script>"))
                .previousAnswers(List.of(
                        PreviousAnswerDTO.builder().questionId(" power-source ").answer(" AC-100v ").build()))
                .consentLogging(true)
                .build();

        when(hsReferenceRepository.searchByToken(any())).thenReturn(List.of());

        hsResolverService.resolveHsCode(request);

        ArgumentCaptor<HsResolveRequestDTO> sanitizedRequestCaptor = ArgumentCaptor.forClass(HsResolveRequestDTO.class);
        ArgumentCaptor<Boolean> consentCaptor = ArgumentCaptor.forClass(Boolean.class);
        //noinspection DataFlowIssue
        verify(hsChatSessionLogService, times(1))
                .recordInteraction(anyString(), consentCaptor.capture(),
                        sanitizedRequestCaptor.capture(), any(HsResolveResponseDTO.class));

        Boolean capturedConsent = consentCaptor.getValue();
        assertNotNull(capturedConsent);
        assertTrue(capturedConsent);

        HsResolveRequestDTO sanitized = sanitizedRequestCaptor.getValue();
        assertEquals("<b>Laser</b> Cutter", sanitized.getProductName());
        assertEquals("Line1 Line2 <script>alert('x')</script>", sanitized.getDescription());
        assertEquals(List.of("industrial", "laser", "<script>"), sanitized.getAttributes());
        assertEquals(List.of(PreviousAnswerDTO.builder()
                .questionId("power-source")
                .answer("AC-100v")
                .build()), sanitized.getPreviousAnswers());
    }
}

