package com.Shakwa.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for requesting password reset
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to send password reset email")
public class PasswordResetRequest {
    
    @Schema(description = "Email address of the user", example = "user@example.com", required = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
}

