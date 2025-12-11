package com.Shakwa.complaint.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for responding to an information request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response to an information request")
public class InformationRequestResponseDTO {

    @Schema(description = "Response message from citizen", 
            example = "إليكم الصورة المطلوبة")
    private String responseMessage;
}

