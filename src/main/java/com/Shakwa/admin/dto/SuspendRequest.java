package com.Shakwa.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for suspending a citizen
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to suspend a citizen")
public class SuspendRequest {
    
    @Schema(description = "Reason for suspension", example = "Violation of terms of service", required = true)
    @NotBlank(message = "Reason is required")
    private String reason;
}

