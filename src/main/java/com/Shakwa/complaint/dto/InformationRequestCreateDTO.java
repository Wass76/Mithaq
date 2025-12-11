package com.Shakwa.complaint.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new information request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create an information request")
public class InformationRequestCreateDTO {

    @Schema(description = "Request message explaining what information is needed", 
            example = "نحتاج إلى صورة واضحة للوثيقة المرفقة")
    @NotBlank(message = "Request message is required")
    private String message;
}

