package com.Shakwa.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for password change request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to change password")
public class ChangePasswordRequest {
    
    @Schema(description = "Current password", example = "oldPassword123", required = true)
    @NotBlank(message = "Current password is required")
    private String oldPassword;
    
    @Schema(description = "New password (minimum 6 characters)", example = "newPassword123", required = true)
    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "New password must be at least 6 characters long")
    private String newPassword;
}
