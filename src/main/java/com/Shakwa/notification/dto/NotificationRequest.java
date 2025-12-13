package com.Shakwa.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for sending notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @NotNull(message = "User ID is required")
    private Long userId;
    
    /**
     * Type of user (USER, CITIZEN, EMPLOYEE). Defaults to CITIZEN if not specified.
     */
    private String userType;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Body is required")
    private String body;

    private String imageUrl; // Optional image URL

    private Map<String, String> data; // Additional data payload

    private String notificationType; // e.g., "complaint_update", "system_alert"

    private String clickAction; // URL or action to perform on click
}
