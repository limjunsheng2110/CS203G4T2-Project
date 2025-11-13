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
    @Size(min = 3, max = 150, message = "Product name must be between 3-150 characters")
    private String productName;

    @NotBlank(message = "Please provide a product description to help identify the HS code")
    @Size(min = 10, max = 2000, message = "Please describe your product in 10-2000 characters")
    private String description;

    private List<@Size(max = 60, message = "Attribute text too long") String> attributes;

    private List<@Valid PreviousAnswerDTO> previousAnswers;

    private String sessionId;

    private Boolean consentLogging;
}

