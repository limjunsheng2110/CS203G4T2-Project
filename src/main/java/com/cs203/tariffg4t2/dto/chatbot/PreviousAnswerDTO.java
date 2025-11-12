package com.cs203.tariffg4t2.dto.chatbot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviousAnswerDTO {

    @NotBlank(message = "Question ID is required")
    @Size(max = 64)
    private String questionId;

    @NotBlank(message = "Answer cannot be blank")
    @Size(max = 200)
    private String answer;
}

