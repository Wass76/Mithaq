package com.Shakwa.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for password reset request (admin only)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to reset password (admin only)")
public class ResetPasswordRequest {
    
    @Schema(description = "ID of the user whose password will be reset", example = "1", required = true)
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @Schema(description = "New password (minimum 6 characters)", example = "newPassword123", required = true)
    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "New password must be at least 6 characters long")
    private String newPassword;
}

