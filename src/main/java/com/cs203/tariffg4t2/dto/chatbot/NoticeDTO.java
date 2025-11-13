package com.cs203.tariffg4t2.dto.chatbot;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeDTO {

    @Size(max = 200)
    private String message;

    @Size(max = 300)
    private String privacyPolicyUrl;

    private Boolean consentGranted;
}

