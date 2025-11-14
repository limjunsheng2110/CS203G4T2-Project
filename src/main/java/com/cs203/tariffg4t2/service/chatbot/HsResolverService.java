package com.cs203.tariffg4t2.service.chatbot;

import com.cs203.tariffg4t2.dto.chatbot.DisambiguationQuestionDTO;
import com.cs203.tariffg4t2.dto.chatbot.FallbackInfoDTO;
import com.cs203.tariffg4t2.dto.chatbot.HsCandidateDTO;
import com.cs203.tariffg4t2.dto.chatbot.HsResolveRequestDTO;
import com.cs203.tariffg4t2.dto.chatbot.HsResolveResponseDTO;
import com.cs203.tariffg4t2.dto.chatbot.NoticeDTO;
import com.cs203.tariffg4t2.dto.chatbot.PreviousAnswerDTO;
import com.cs203.tariffg4t2.model.basic.Product;
import com.cs203.tariffg4t2.repository.basic.ProductRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class HsResolverService {

    private static final Logger logger = LoggerFactory.getLogger(HsResolverService.class);

    private final ProductRepository productRepository;
    private final HsChatSessionLogService hsChatSessionLogService;

    public HsResolveResponseDTO resolveHsCode(HsResolveRequestDTO request) {
        String sessionId = request.getSessionId() != null
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        String queryId = request.getQueryId() != null
                ? request.getQueryId()
                : UUID.randomUUID().toString();

        logger.debug("Resolving HS code for session={} query={}", sessionId, queryId);

        HsResolveRequestDTO sanitizedRequest = sanitizeRequest(request);

        List<HsCandidateDTO> candidates = buildCandidatesFromReferenceData(sanitizedRequest);
        // Don't return fallback mock - just return empty list if no matches found
        List<DisambiguationQuestionDTO> disambiguationQuestions = buildDisambiguationQuestions(candidates);
        FallbackInfoDTO fallback = buildFallbackInfo();

        // No notice needed - remove privacy message
        NoticeDTO notice = null;

        HsResolveResponseDTO response = HsResolveResponseDTO.builder()
                .queryId(queryId)
                .sessionId(sessionId)
                .candidates(candidates)
                .disambiguationQuestions(disambiguationQuestions)
                .fallback(fallback)
                .notice(notice)
                .build();

        hsChatSessionLogService.recordInteraction(
                Objects.requireNonNull(sessionId, "sessionId"),
                Boolean.TRUE.equals(sanitizedRequest.getConsentLogging()),
                sanitizedRequest,
                response
        );

        return response;
    }

    private List<HsCandidateDTO> buildCandidatesFromReferenceData(HsResolveRequestDTO request) {
        Set<String> tokens = deriveSearchTokens(request);
        if (tokens.isEmpty()) {
            return List.of();
        }

        Map<String, CandidateScore> scoreMap = new LinkedHashMap<>();

        for (String token : tokens) {
            List<Product> matches = productRepository.searchByToken(token);
            for (Product product : matches) {
                CandidateScore score = scoreMap.computeIfAbsent(product.getHsCode(), code ->
                        new CandidateScore(product));
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
                        .hsCode(score.getProduct().getHsCode())
                        .confidence(score.getConfidence())
                        .rationale(score.getProduct().getDescription())
                        .source("PRODUCT_DATABASE")
                        .attributesUsed(score.getMatchedTokens())
                        .build())
                .toList();
    }

    private List<DisambiguationQuestionDTO> buildDisambiguationQuestions(List<HsCandidateDTO> candidates) {
        // Disable disambiguation questions for now - let users refine with natural language
        // This avoids hardcoded questions that may not be relevant to actual products
        return List.of();
    }

    private FallbackInfoDTO buildFallbackInfo() {
        // Return empty fallback - no fake previous codes or hardcoded URLs
        return FallbackInfoDTO.builder()
                .lastUsedCodes(List.of())
                .manualSearchUrl(null)
                .build();
    }

    private Set<String> deriveSearchTokens(HsResolveRequestDTO request) {
        String combined = String.format("%s %s %s",
                safe(request.getProductName()),
                safe(request.getDescription()),
                request.getAttributes() != null ? String.join(" ", request.getAttributes()) : "");

        return Arrays.stream(combined.toLowerCase(Locale.ENGLISH).split("\\W+"))
                .filter(token -> token.length() > 3)
                .limit(12)
                .collect(Collectors.toSet());
    }

    private HsResolveRequestDTO sanitizeRequest(HsResolveRequestDTO request) {
        List<String> sanitizedAttributes = request.getAttributes() == null ? null :
                request.getAttributes().stream()
                        .map(attr -> normalize(attr, 60))
                        .filter(attr -> !attr.isBlank())
                        .toList();

        List<PreviousAnswerDTO> sanitizedAnswers = request.getPreviousAnswers() == null ? null :
                request.getPreviousAnswers().stream()
                        .map(answer -> PreviousAnswerDTO.builder()
                                .questionId(normalize(answer.getQuestionId(), 64))
                                .answer(normalize(answer.getAnswer(), 200))
                                .build())
                        .toList();

        return HsResolveRequestDTO.builder()
                .sessionId(request.getSessionId())
                .queryId(request.getQueryId())
                .productName(normalize(request.getProductName(), 150))
                .description(normalize(request.getDescription(), 2000))
                .attributes(sanitizedAttributes)
                .previousAnswers(sanitizedAnswers)
                .consentLogging(request.getConsentLogging())
                .build();
    }

    private String safe(String input) {
        return input == null ? "" : input;
    }

    private String normalize(String input, int maxLength) {
        String cleaned = safe(input).replaceAll("[\\r\\n\\t]+", " ").trim();
        if (cleaned.length() > maxLength) {
            return cleaned.substring(0, maxLength);
        }
        return cleaned;
    }


    private static class CandidateScore {
        private final Product product;
        private final List<String> matchedTokens = new ArrayList<>();
        private int matchCount = 0;
        private double confidence = 0.0;

        CandidateScore(Product product) {
            this.product = product;
        }

        void incrementMatchCount(String token) {
            matchedTokens.add(token);
            matchCount++;
        }

        void calculateConfidence(int totalTokens) {
            double base = 0.45;
            double tokenCoverage = totalTokens > 0 ? (double) matchCount / totalTokens : 0;
            double descriptionLengthFactor = Math.min(0.25, product.getDescription().length() / 1000.0);
            this.confidence = Math.min(0.95, base + (tokenCoverage * 0.45) + descriptionLengthFactor);
        }

        public Product getProduct() {
            return product;
        }

        public double getConfidence() {
            return confidence;
        }

        public List<String> getMatchedTokens() {
            return matchedTokens;
        }
    }
}

