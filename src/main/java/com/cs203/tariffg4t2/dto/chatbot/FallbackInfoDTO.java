package com.cs203.tariffg4t2.dto.chatbot;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FallbackInfoDTO {

    private List<PreviousHsSelectionDTO> lastUsedCodes;

    @Size(max = 300)
    private String manualSearchUrl;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreviousHsSelectionDTO {

        @Pattern(regexp = "^[0-9]{4}.[0-9]{2}.[0-9]{2}$")
        private String hsCode;

        private Double confidence;

        private Instant timestamp;
    }
}

