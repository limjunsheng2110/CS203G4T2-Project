package com.cs203.tariffg4t2.dto.chatbot;

import jakarta.validation.constraints.NotBlank;
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
public class DisambiguationQuestionDTO {

    @NotBlank
    @Size(max = 64)
    private String id;

    @NotBlank
    @Size(max = 200)
    private String question;

    private List<@Size(max = 60) String> options;
}

