package com.Shakwa.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for registering FCM tokens
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRegistrationRequest {

    @NotBlank(message = "Token is required")
    private String token;

    private String deviceType; // e.g., "web", "android", "ios"

    private String deviceInfo; // Additional device information
}
