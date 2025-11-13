package com.cs203.tariffg4t2.controller.chatbot;

import com.cs203.tariffg4t2.dto.chatbot.HsResolveRequestDTO;
import com.cs203.tariffg4t2.dto.chatbot.HsResolveResponseDTO;
import com.cs203.tariffg4t2.service.chatbot.HsRateLimiter;
import com.cs203.tariffg4t2.service.chatbot.HsResolverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/hs")
@Validated
@RequiredArgsConstructor
public class HsResolverController {

    private static final Logger logger = LoggerFactory.getLogger(HsResolverController.class);

    private final HsResolverService hsResolverService;
    private final HsRateLimiter hsRateLimiter;

    @PostMapping("/resolve")
    public ResponseEntity<HsResolveResponseDTO> resolveHsCode(
            @Valid @RequestBody HsResolveRequestDTO request,
            HttpServletRequest httpServletRequest,
            @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor
    ) {
        logger.info("HS resolve request received queryId={} sessionId={}",
                request.getQueryId(), request.getSessionId());

        String rateLimitKey = buildRateLimitKey(request, httpServletRequest, forwardedFor);
        if (!hsRateLimiter.tryConsume(rateLimitKey)) {
            logger.warn("Rate limit triggered for key={}", rateLimitKey);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        HsResolveResponseDTO response = hsResolverService.resolveHsCode(request);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private String buildRateLimitKey(HsResolveRequestDTO request,
                                     HttpServletRequest httpServletRequest,
                                     String forwardedFor) {
        if (request.getSessionId() != null) {
            return "session:" + request.getSessionId();
        }

        String ipAddress = extractClientIp(forwardedFor, httpServletRequest);
        return "ip:" + ipAddress;
    }

    private String extractClientIp(String forwardedFor, HttpServletRequest request) {
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

