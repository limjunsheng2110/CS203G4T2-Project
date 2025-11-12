package com.cs203.tariffg4t2.controller.chatbot;

import com.cs203.tariffg4t2.dto.chatbot.HsResolveRequestDTO;
import com.cs203.tariffg4t2.dto.chatbot.HsResolveResponseDTO;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hs")
@Validated
@RequiredArgsConstructor
public class HsResolverController {

    private static final Logger logger = LoggerFactory.getLogger(HsResolverController.class);

    private final HsResolverService hsResolverService;

    @PostMapping("/resolve")
    public ResponseEntity<HsResolveResponseDTO> resolveHsCode(
            @Valid @RequestBody HsResolveRequestDTO request
    ) {
        logger.info("HS resolve request received queryId={} sessionId={}",
                request.getQueryId(), request.getSessionId());

        HsResolveResponseDTO response = hsResolverService.resolveHsCode(request);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}

