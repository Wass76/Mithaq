package com.Shakwa.notification.service;

import com.Shakwa.notification.dto.NotificationRequest;
import com.Shakwa.user.entity.BaseUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending security-related notifications
 * Handles notifications for rate limiting, suspicious activity, etc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityNotificationService {

    private final NotificationService notificationService;

    /**
     * Send notification when user hits rate limit for login attempts
     */
    public void notifyRateLimitExceeded(BaseUser user, String ipAddress) {
        if (user == null) {
            log.warn("Cannot send rate limit notification: user is null");
            return;
        }

        String title = "تنبيه أمني: محاولات تسجيل دخول متعددة";
        String body = String.format(
            "تم حظر محاولات تسجيل الدخول المتعددة من عنوان IP: %s. يرجى المحاولة مرة أخرى لاحقاً.",
            maskIpAddress(ipAddress)
        );

        Map<String, String> data = new HashMap<>();
        data.put("type", "rate_limit_exceeded");
        data.put("ipAddress", maskIpAddress(ipAddress));
        data.put("action", "login_blocked");

        NotificationRequest request = NotificationRequest.builder()
                .userId(user.getId())
                .title(title)
                .body(body)
                .data(data)
                .notificationType("security_alert")
                .clickAction("/security/alerts")
                .build();

        try {
            notificationService.sendNotification(request);
            log.info("Rate limit notification sent for user {} from IP {}", user.getId(), maskIpAddress(ipAddress));
        } catch (Exception e) {
            log.error("Failed to send rate limit notification for user {}: {}", 
                    user.getId(), e.getMessage(), e);
        }
    }

    /**
     * Mask IP address for privacy (show only first two octets)
     */
    private String maskIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return "unknown";
        }
        String[] parts = ipAddress.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1] + ".xxx.xxx";
        }
        return "xxx.xxx.xxx.xxx";
    }
}

