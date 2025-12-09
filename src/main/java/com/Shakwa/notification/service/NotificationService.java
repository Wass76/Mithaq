package com.Shakwa.notification.service;

import com.Shakwa.notification.dto.NotificationRequest;
import com.Shakwa.notification.dto.NotificationResponse;
import com.Shakwa.notification.dto.TokenRegistrationRequest;
import com.Shakwa.notification.entity.Notification;
import com.Shakwa.notification.entity.NotificationToken;
import com.Shakwa.notification.repository.NotificationRepository;
import com.Shakwa.user.entity.User;
import com.Shakwa.utils.annotation.Audited;
import com.Shakwa.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing notifications
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final FirebaseNotificationService firebaseNotificationService;
    private final NotificationQueueService notificationQueueService;

    /**
     * Register FCM token for current user
     */
    @Audited(action = "REGISTER_NOTIFICATION_TOKEN", targetType = "NOTIFICATION_TOKEN", includeArgs = false)
    @Transactional
    public NotificationToken registerToken(User user, TokenRegistrationRequest request) {
        return firebaseNotificationService.registerToken(
                user,
                request.getToken(),
                request.getDeviceType(),
                request.getDeviceInfo()
        );
    }

    /**
     * Unregister FCM token
     */
    @Audited(action = "UNREGISTER_NOTIFICATION_TOKEN", targetType = "NOTIFICATION_TOKEN", includeArgs = false)
    @Transactional
    public void unregisterToken(User user, String token) {
        firebaseNotificationService.unregisterToken(user, token);
    }

    /**
     * Get all notifications for a user
     */
    public Page<NotificationResponse> getUserNotifications(User user, Pageable pageable) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(NotificationResponse::fromEntity);
    }

    /**
     * Get unread notifications for a user
     */
    public List<NotificationResponse> getUnreadNotifications(User user) {
        return notificationRepository.findUnreadNotificationsByUser(user).stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    /**
     * Get unread notification count
     */
    public Long getUnreadCount(User user) {
        return notificationRepository.countUnreadNotificationsByUser(user);
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public void markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Notification does not belong to user");
        }

        if (notification.getReadAt() == null) {
            notificationRepository.markAsRead(notificationId, LocalDateTime.now());
        }
    }

    /**
     * Mark all notifications as read for a user
     */
    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsReadByUser(user, LocalDateTime.now());
    }

    /**
     * Send notification to a user
     */
    @Audited(action = "SEND_NOTIFICATION", targetType = "NOTIFICATION", includeArgs = false)
    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) {
        Notification notification = firebaseNotificationService.createPendingNotification(request);
        enqueueAfterCommit(notification.getId(), request);
        return NotificationResponse.fromEntity(notification);
    }

    /**
     * Send notification to multiple users
     */
    @Transactional
    public void sendNotificationToMultipleUsers(List<Long> userIds, NotificationRequest request) {
        for (Long userId : userIds) {
            NotificationRequest userRequest = NotificationRequest.builder()
                    .userId(userId)
                    .title(request.getTitle())
                    .body(request.getBody())
                    .imageUrl(request.getImageUrl())
                    .data(request.getData())
                    .notificationType(request.getNotificationType())
                    .clickAction(request.getClickAction())
                    .build();
            Notification notification = firebaseNotificationService.createPendingNotification(userRequest);
            enqueueAfterCommit(notification.getId(), userRequest);
        }
    }

    private void enqueueAfterCommit(Long notificationId, NotificationRequest request) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationQueueService.enqueue(notificationId, request);
                }
            });
        } else {
            notificationQueueService.enqueue(notificationId, request);
        }
    }
}
