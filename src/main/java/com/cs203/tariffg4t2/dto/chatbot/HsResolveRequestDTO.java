package com.cs203.tariffg4t2.dto.chatbot;

import jakarta.validation.Valid;
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
public class HsResolveRequestDTO {

    private String queryId;

    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 150, message = "Product name must be 3-150 characters")
    private String productName;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be 10-2000 characters")
    private String description;

    private List<@Size(max = 60, message = "Attribute text too long") String> attributes;

    private List<@Valid PreviousAnswerDTO> previousAnswers;

    private String sessionId;

    private Boolean consentLogging;
}

