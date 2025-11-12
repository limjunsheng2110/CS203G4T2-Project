package com.cs203.tariffg4t2.service.chatbot;

import com.cs203.tariffg4t2.dto.chatbot.HsResolveRequestDTO;
import com.cs203.tariffg4t2.dto.chatbot.HsResolveResponseDTO;
import com.cs203.tariffg4t2.model.chatbot.HsChatSessionLog;
import com.cs203.tariffg4t2.repository.chatbot.HsChatSessionLogRepository;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class HsChatSessionLogService {

    private final HsChatSessionLogRepository hsChatSessionLogRepository;

    @Transactional
    public void recordInteraction(String sessionId,
                                  boolean consentLogging,
                                  HsResolveRequestDTO requestDTO,
                                  HsResolveResponseDTO responseDTO) {
        if (!consentLogging) {
            return;
        }

        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        try {
            HsChatSessionLog sessionLog = hsChatSessionLogRepository.findById(sessionId)
                    .orElse(HsChatSessionLog.builder()
                            .sessionId(sessionId)
                            .consentLogging(true)
                            .requestCount(0)
                            .createdAt(Instant.now())
                            .build());

            sessionLog.setConsentLogging(true);
            int newCount = sessionLog.getRequestCount() == null ? 1 : sessionLog.getRequestCount() + 1;
            sessionLog.setRequestCount(newCount);
            sessionLog.setUpdatedAt(Instant.now());
            sessionLog.setLastProductName(truncateAndEscape(requestDTO.getProductName(), 150));

            if (responseDTO.getCandidates() != null && !responseDTO.getCandidates().isEmpty()) {
                sessionLog.setLastHsCode(responseDTO.getCandidates().get(0).getHsCode());
            }

            hsChatSessionLogRepository.save(sessionLog);
        } catch (Exception e) {
            log.warn("Failed to persist HS chat session log for sessionId={}", sessionId, e);
        }
    }

    private String truncateAndEscape(String input, int maxLength) {
        if (input == null) {
            return null;
        }
        String escaped = Objects.requireNonNullElse(HtmlUtils.htmlEscape(input), "");
        return escaped.length() > maxLength ? escaped.substring(0, maxLength) : escaped;
    }
}

