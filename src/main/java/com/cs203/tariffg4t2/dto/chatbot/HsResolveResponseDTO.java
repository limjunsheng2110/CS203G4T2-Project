package com.cs203.tariffg4t2.dto.chatbot;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HsResolveResponseDTO {

    private String queryId;
    private String sessionId;
    private List<HsCandidateDTO> candidates;
    private List<DisambiguationQuestionDTO> disambiguationQuestions;
    private FallbackInfoDTO fallback;
    private NoticeDTO notice;
}

