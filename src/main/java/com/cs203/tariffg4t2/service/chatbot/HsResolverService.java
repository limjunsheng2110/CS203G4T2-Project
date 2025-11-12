package com.cs203.tariffg4t2.service.chatbot;

import com.cs203.tariffg4t2.dto.chatbot.DisambiguationQuestionDTO;
import com.cs203.tariffg4t2.dto.chatbot.FallbackInfoDTO;
import com.cs203.tariffg4t2.dto.chatbot.HsCandidateDTO;
import com.cs203.tariffg4t2.dto.chatbot.HsResolveRequestDTO;
import com.cs203.tariffg4t2.dto.chatbot.HsResolveResponseDTO;
import com.cs203.tariffg4t2.dto.chatbot.NoticeDTO;
import com.cs203.tariffg4t2.model.chatbot.HsReference;
import com.cs203.tariffg4t2.repository.chatbot.HsReferenceRepository;
import java.util.Arrays;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HsResolverService {

    private static final Logger logger = LoggerFactory.getLogger(HsResolverService.class);

    private static final double PRIMARY_CONFIDENCE = 0.82;
    private static final double SECONDARY_CONFIDENCE = 0.74;
    private static final double DISAMBIGUATION_THRESHOLD = 0.1;

    private final HsReferenceRepository hsReferenceRepository;

    public HsResolveResponseDTO resolveHsCode(HsResolveRequestDTO request) {
        String sessionId = request.getSessionId() != null
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        String queryId = request.getQueryId() != null
                ? request.getQueryId()
                : UUID.randomUUID().toString();

        logger.debug("Resolving HS code for session={} query={}", sessionId, queryId);

        List<HsCandidateDTO> candidates = buildCandidatesFromReferenceData(request);
        if (candidates.isEmpty()) {
            candidates = buildCandidateMockFallback(request);
        }
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

    private List<HsCandidateDTO> buildCandidatesFromReferenceData(HsResolveRequestDTO request) {
        Set<String> tokens = deriveSearchTokens(request);
        if (tokens.isEmpty()) {
            return List.of();
        }

        Map<String, CandidateScore> scoreMap = new LinkedHashMap<>();

        for (String token : tokens) {
            List<HsReference> matches = hsReferenceRepository.searchByToken(token);
            for (HsReference reference : matches) {
                CandidateScore score = scoreMap.computeIfAbsent(reference.getHsCode(), code ->
                        new CandidateScore(reference));
                score.incrementMatchCount(token);
            }
        }

        if (scoreMap.isEmpty()) {
            return List.of();
        }

        int totalTokens = tokens.size();
        List<CandidateScore> rankedScores = scoreMap.values().stream()
                .peek(candidateScore -> candidateScore.calculateConfidence(totalTokens))
                .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
                .limit(3)
                .toList();

        return rankedScores.stream()
                .map(score -> HsCandidateDTO.builder()
                        .hsCode(score.getReference().getHsCode())
                        .confidence(score.getConfidence())
                        .rationale(score.getReference().getDescription())
                        .source("REFERENCE")
                        .attributesUsed(score.getMatchedTokens())
                        .build())
                .toList();
    }

    private List<HsCandidateDTO> buildCandidateMockFallback(HsResolveRequestDTO request) {
        List<String> attributes = request.getAttributes() != null ? request.getAttributes() : List.of();

        return List.of(HsCandidateDTO.builder()
                .hsCode("0000.00.00")
                .confidence(0.55)
                .rationale("Insufficient data to provide a specific HS code — please provide more details.")
                .source("RULE")
                .attributesUsed(attributes)
                .build());
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

    private Set<String> deriveSearchTokens(HsResolveRequestDTO request) {
        String combined = String.format("%s %s %s",
                request.getProductName(),
                request.getDescription(),
                request.getAttributes() != null ? String.join(" ", request.getAttributes()) : "");

        return Arrays.stream(combined.toLowerCase(Locale.ENGLISH).split("\\W+"))
                .filter(token -> token.length() > 3)
                .limit(12)
                .collect(Collectors.toSet());
    }

    private static class CandidateScore {
        private final HsReference reference;
        private final List<String> matchedTokens = new ArrayList<>();
        private int matchCount = 0;
        private double confidence = 0.0;

        CandidateScore(HsReference reference) {
            this.reference = reference;
        }

        void incrementMatchCount(String token) {
            matchedTokens.add(token);
            matchCount++;
        }

        void calculateConfidence(int totalTokens) {
            double base = 0.45;
            double tokenCoverage = totalTokens > 0 ? (double) matchCount / totalTokens : 0;
            double descriptionLengthFactor = Math.min(0.25, reference.getDescription().length() / 1000.0);
            this.confidence = Math.min(0.95, base + (tokenCoverage * 0.45) + descriptionLengthFactor);
        }

        public HsReference getReference() {
            return reference;
        }

        public double getConfidence() {
            return confidence;
        }

        public List<String> getMatchedTokens() {
            return matchedTokens;
        }
    }
}

