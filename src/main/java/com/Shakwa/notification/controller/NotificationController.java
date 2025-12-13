package com.Shakwa.notification.controller;

import com.Shakwa.notification.dto.NotificationRequest;
import com.Shakwa.notification.dto.NotificationResponse;
import com.Shakwa.notification.dto.TokenRegistrationRequest;
import com.Shakwa.notification.entity.NotificationToken;
import com.Shakwa.notification.service.NotificationService;
import com.Shakwa.user.entity.BaseUser;
import com.Shakwa.utils.controller.BaseController;
import com.Shakwa.utils.response.ApiResponseClass;
import com.Shakwa.utils.response.PaginationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for notification management
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController extends BaseController {

    private final NotificationService notificationService;

    /**
     * Register FCM token for current user
     * POST /api/v1/notifications/register-token
     */
    @PostMapping("/register-token")
    public ResponseEntity<?> registerToken(
            @Valid @RequestBody TokenRegistrationRequest request,
            Authentication authentication) {
        
        BaseUser user = (BaseUser) authentication.getPrincipal();
        NotificationToken token = notificationService.registerToken(user, request);
        
        return ResponseEntity.ok(
                ApiResponseClass.builder()
                        .body(token)
                        .message("Token registered successfully")
                        .build()
        );
    }

    /**
     * Unregister FCM token
     * DELETE /api/v1/notifications/unregister-token/{token}
     */
    @DeleteMapping("/unregister-token/{token}")
    public ResponseEntity<?> unregisterToken(
            @PathVariable String token,
            Authentication authentication) {
        
        BaseUser user = (BaseUser) authentication.getPrincipal();
        notificationService.unregisterToken(user, token);
        
        return ResponseEntity.ok(
                ApiResponseClass.builder()
                        .message("Token unregistered successfully")
                        .build()
        );
    }

    /**
     * Get all notifications for current user
     * GET /api/v1/notifications
     */
    @GetMapping
    public ResponseEntity<?> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            Authentication authentication) {
        
        BaseUser user = (BaseUser) authentication.getPrincipal();
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? 
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        var notificationPage = notificationService.getUserNotifications(user, pageable);
        PaginationResponse paginationResponse = PaginationResponse.builder()
                .page(notificationPage.getNumber())
                .perPage(notificationPage.getSize())
                .total(notificationPage.getTotalElements())
                .build();
        
        return sendResponse(notificationPage.getContent(), "Notifications retrieved successfully", 
                HttpStatus.OK, paginationResponse);
    }

    /**
     * Get unread notifications
     * GET /api/v1/notifications/unread
     */
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(
            Authentication authentication) {
        
        BaseUser user = (BaseUser) authentication.getPrincipal();
        List<NotificationResponse> notifications = notificationService.getUnreadNotifications(user);
        
        return ResponseEntity.ok(
                ApiResponseClass.builder()
                        .body(notifications)
                        .build()
        );
    }

    /**
     * Get unread notification count
     * GET /api/v1/notifications/unread/count
     */
    @GetMapping("/unread/count")
    public ResponseEntity<?> getUnreadCount(
            Authentication authentication) {
        
        BaseUser user = (BaseUser) authentication.getPrincipal();
        Long count = notificationService.getUnreadCount(user);
        
        return ResponseEntity.ok(
                ApiResponseClass.builder()
                        .body(Map.of("count", count))
                        .build()
        );
    }

    /**
     * Mark notification as read
     * PUT /api/v1/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        
        BaseUser user = (BaseUser) authentication.getPrincipal();
        notificationService.markAsRead(id, user);
        
        return ResponseEntity.ok(
                ApiResponseClass.builder()
                        .message("Notification marked as read")
                        .build()
        );
    }

    /**
     * Mark all notifications as read
     * PUT /api/v1/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(
            Authentication authentication) {
        
        BaseUser user = (BaseUser) authentication.getPrincipal();
        notificationService.markAllAsRead(user);
        
        return ResponseEntity.ok(
                ApiResponseClass.builder()
                        .message("All notifications marked as read")
                        .build()
        );
    }

    /**
     * Send notification (Admin only - can be secured with @PreAuthorize)
     * POST /api/v1/notifications/send
     */
    @PostMapping("/send")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<?> sendNotification(
            @Valid @RequestBody NotificationRequest request) {
        
        NotificationResponse response = notificationService.sendNotification(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponseClass.builder()
                        .body(response)
                        .message("Notification sent successfully")
                        .build()
        );
    }
}
