package com.cs203.tariffg4t2.service.chatbot;

import com.cs203.tariffg4t2.dto.chatbot.DisambiguationQuestionDTO;
import com.cs203.tariffg4t2.dto.chatbot.FallbackInfoDTO;
import com.cs203.tariffg4t2.dto.chatbot.HsCandidateDTO;
import com.cs203.tariffg4t2.dto.chatbot.HsResolveRequestDTO;
import com.cs203.tariffg4t2.dto.chatbot.HsResolveResponseDTO;
import com.cs203.tariffg4t2.dto.chatbot.NoticeDTO;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HsResolverService {

    private static final Logger logger = LoggerFactory.getLogger(HsResolverService.class);

    private static final double PRIMARY_CONFIDENCE = 0.82;
    private static final double SECONDARY_CONFIDENCE = 0.74;
    private static final double DISAMBIGUATION_THRESHOLD = 0.1;

    public HsResolveResponseDTO resolveHsCode(HsResolveRequestDTO request) {
        String sessionId = request.getSessionId() != null
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        String queryId = request.getQueryId() != null
                ? request.getQueryId()
                : UUID.randomUUID().toString();

        logger.debug("Resolving HS code for session={} query={}", sessionId, queryId);

        List<HsCandidateDTO> candidates = buildCandidateMockResponses(request);
        List<DisambiguationQuestionDTO> disambiguationQuestions = buildDisambiguationQuestions(candidates);
        FallbackInfoDTO fallback = buildFallbackInfo();

        NoticeDTO notice = NoticeDTO.builder()
                .message("We may store chat history to improve results.")
                .privacyPolicyUrl("/privacy")
                .consentGranted(Boolean.TRUE.equals(request.getConsentLogging()))
                .build();

        return HsResolveResponseDTO.builder()
                .queryId(queryId)
                .sessionId(sessionId)
                .candidates(candidates)
                .disambiguationQuestions(disambiguationQuestions)
                .fallback(fallback)
                .notice(notice)
                .build();
    }

    private List<HsCandidateDTO> buildCandidateMockResponses(HsResolveRequestDTO request) {
        String normalizedName = request.getProductName().toLowerCase();
        List<String> attributes = request.getAttributes() != null ? request.getAttributes() : List.of();

        List<HsCandidateDTO> candidates = new ArrayList<>();

        if (normalizedName.contains("laptop") || normalizedName.contains("computer")) {
            candidates.add(HsCandidateDTO.builder()
                    .hsCode("8471.30.01")
                    .confidence(PRIMARY_CONFIDENCE)
                    .rationale("Portable automatic data processing machine with keyboard and display.")
                    .source("HYBRID")
                    .attributesUsed(List.of("portable", "data processing", "with display"))
                    .build());

            candidates.add(HsCandidateDTO.builder()
                    .hsCode("8471.41.00")
                    .confidence(SECONDARY_CONFIDENCE)
                    .rationale("Automatic data processing units without an integrated display.")
                    .source("HYBRID")
                    .attributesUsed(List.of("data processing"))
                    .build());
        } else if (normalizedName.contains("textile") || normalizedName.contains("fabric")) {
            candidates.add(HsCandidateDTO.builder()
                    .hsCode("5512.99.00")
                    .confidence(PRIMARY_CONFIDENCE)
                    .rationale("Woven fabrics of synthetic staple fibres, mixed materials.")
                    .source("RULE")
                    .attributesUsed(List.of("synthetic fibre", "woven fabric"))
                    .build());

            candidates.add(HsCandidateDTO.builder()
                    .hsCode("5903.20.00")
                    .confidence(SECONDARY_CONFIDENCE)
                    .rationale("Textile fabrics impregnated or coated with polyurethane.")
                    .source("RULE")
                    .attributesUsed(List.of("coated fabric"))
                    .build());
        } else {
            candidates.add(HsCandidateDTO.builder()
                    .hsCode("0000.00.00")
                    .confidence(0.55)
                    .rationale("Insufficient data to provide a specific HS code — please provide more details.")
                    .source("RULE")
                    .attributesUsed(attributes)
                    .build());
        }

        return candidates;
    }

    private List<DisambiguationQuestionDTO> buildDisambiguationQuestions(List<HsCandidateDTO> candidates) {
        if (candidates.size() < 2) {
            return List.of();
        }

        double topConfidence = candidates.get(0).getConfidence();
        double secondConfidence = candidates.get(1).getConfidence();

        if (Math.abs(topConfidence - secondConfidence) > DISAMBIGUATION_THRESHOLD) {
            return List.of();
        }

        return List.of(
                DisambiguationQuestionDTO.builder()
                        .id("power-source")
                        .question("Does the product operate without an external power supply?")
                        .options(List.of("Yes", "No", "Not sure"))
                        .build(),
                DisambiguationQuestionDTO.builder()
                        .id("material-primary")
                        .question("What is the primary casing material?")
                        .options(List.of("Plastic", "Metal", "Composite", "Other"))
                        .build()
        );
    }

    private FallbackInfoDTO buildFallbackInfo() {
        FallbackInfoDTO.PreviousHsSelectionDTO previous = FallbackInfoDTO.PreviousHsSelectionDTO.builder()
                .hsCode("8471.30.01")
                .confidence(0.78)
                .timestamp(Instant.now().minusSeconds(3600))
                .build();

        return FallbackInfoDTO.builder()
                .lastUsedCodes(List.of(previous))
                .manualSearchUrl("https://hts.usitc.gov/")
                .build();
    }
}

