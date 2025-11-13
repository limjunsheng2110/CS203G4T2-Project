package com.cs203.tariffg4t2.dto.chatbot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HsCandidateDTO {

    @NotBlank
    @Pattern(regexp = "^[0-9]{4}.[0-9]{2}.[0-9]{2}$", message = "HS code must be in format ####.##.##")
    private String hsCode;

    @NotNull
    private Double confidence;

    @NotBlank
    @Size(max = 500)
    private String rationale;

    @NotBlank
    private String source;

    private List<@Size(max = 50) String> attributesUsed;
}

