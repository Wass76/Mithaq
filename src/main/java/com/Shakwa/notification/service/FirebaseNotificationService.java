package com.Shakwa.notification.service;

import com.Shakwa.notification.dto.NotificationRequest;
import com.Shakwa.notification.dto.NotificationResponse;
import com.Shakwa.notification.entity.Notification;
import com.Shakwa.notification.entity.NotificationToken;
import com.Shakwa.notification.repository.NotificationRepository;
import com.Shakwa.notification.repository.NotificationTokenRepository;
import com.Shakwa.user.entity.User;
import com.Shakwa.user.repository.UserRepository;
import com.google.firebase.messaging.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for sending Firebase Cloud Messaging (FCM) notifications
 */
@Slf4j
@Service
public class FirebaseNotificationService {

    private final FirebaseMessaging firebaseMessaging;
    private final NotificationTokenRepository tokenRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    private final Counter notificationSentCounter;
    private final Counter notificationFailedCounter;

    @Autowired
    public FirebaseNotificationService(FirebaseMessaging firebaseMessaging,
                                       NotificationTokenRepository tokenRepository,
                                       NotificationRepository notificationRepository,
                                       UserRepository userRepository,
                                       ObjectMapper objectMapper,
                                       MeterRegistry meterRegistry) {
        this.firebaseMessaging = firebaseMessaging;
        this.tokenRepository = tokenRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;

        this.notificationSentCounter = meterRegistry.counter("notifications.sent");
        this.notificationFailedCounter = meterRegistry.counter("notifications.failed");
    }

    /**
     * Create a pending notification record that can be dispatched asynchronously.
     */
    @Transactional
    public Notification createPendingNotification(NotificationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUserId()));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(request.getTitle());
        notification.setBody(request.getBody());
        try {
            notification.setData(request.getData() != null ? objectMapper.writeValueAsString(request.getData()) : null);
        } catch (Exception e) {
            log.error("Failed to serialize notification data", e);
            notification.setData(null);
        }
        notification.setNotificationType(request.getNotificationType());
        notification.setStatus(Notification.NotificationStatus.PENDING);

        return notificationRepository.save(notification);
    }

    /**
     * Dispatch an already-created notification record to Firebase.
     */
    @Transactional
    public NotificationResponse deliverNotification(Long notificationId, NotificationRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with ID: " + notificationId));
        User user = notification.getUser();

        List<NotificationToken> tokens = tokenRepository.findActiveTokensByUserId(user.getId());

        if (tokens.isEmpty()) {
            log.warn("No active tokens found for userId={} type={}", user.getId(), notification.getNotificationType());
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setErrorMessage("No active tokens found");
            notificationRepository.save(notification);
            notificationFailedCounter.increment();
            sample.stop(Timer.builder("notifications.dispatch.duration")
                    .tag("type", safeType(notification))
                    .tag("result", "failed")
                    .register(meterRegistry));
            return NotificationResponse.fromEntity(notification);
        }

        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new ArrayList<>();

        for (NotificationToken token : tokens) {
            try {
                sendToToken(token.getToken(), request);
                successCount++;
                tokenRepository.updateLastUsedAt(token.getId(), LocalDateTime.now());
            } catch (Exception e) {
                failureCount++;
                String errorMsg = "Failed to send to token: " + e.getMessage();
                errors.add(errorMsg);
                log.error(errorMsg, e);

                if (e.getMessage().contains("Invalid") || e.getMessage().contains("NotRegistered")) {
                    token.setIsActive(false);
                    tokenRepository.save(token);
                    String errorCode = (e instanceof FirebaseMessagingException fem && fem.getMessagingErrorCode() != null)
                            ? fem.getMessagingErrorCode().name()
                            : "unknown";
                    log.warn("Deactivated tokenId={} for userId={} due to errorCode={}", token.getId(), user.getId(), errorCode);
                }
            }
        }

        if (successCount > 0) {
            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationSentCounter.increment();
        } else {
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setErrorMessage(String.join("; ", errors));
            notificationFailedCounter.increment();
        }
        notificationRepository.save(notification);

        if (successCount > 0 && failureCount == 0) {
            log.info("Notification dispatch success notificationId={} userId={} tokensTried={} success={} failure={}",
                    notificationId, user.getId(), tokens.size(), successCount, failureCount);
        } else if (successCount > 0) {
            log.warn("Notification dispatch partial notificationId={} userId={} tokensTried={} success={} failure={} errors={}",
                    notificationId, user.getId(), tokens.size(), successCount, failureCount, errors);
        } else {
            log.error("Notification dispatch failed notificationId={} userId={} tokensTried={} success={} failure={} errors={}",
                    notificationId, user.getId(), tokens.size(), successCount, failureCount, errors);
        }

        sample.stop(Timer.builder("notifications.dispatch.duration")
                .tag("type", safeType(notification))
                .tag("result", successCount > 0 ? "sent" : "failed")
                .register(meterRegistry));

        return NotificationResponse.fromEntity(notification);
    }

    private String safeType(Notification notification) {
        return notification.getNotificationType() != null ? notification.getNotificationType() : "unknown";
    }

    /**
     * Send notification to multiple users
     */
    @Transactional
    public Map<Long, NotificationResponse> sendNotificationToMultipleUsers(
            List<Long> userIds, NotificationRequest baseRequest) {

        Map<Long, NotificationResponse> results = new HashMap<>();

        for (Long userId : userIds) {
            NotificationRequest request = NotificationRequest.builder()
                    .userId(userId)
                    .title(baseRequest.getTitle())
                    .body(baseRequest.getBody())
                    .imageUrl(baseRequest.getImageUrl())
                    .data(baseRequest.getData())
                    .notificationType(baseRequest.getNotificationType())
                    .clickAction(baseRequest.getClickAction())
                    .build();

            try {
                Notification notification = createPendingNotification(request);
                NotificationResponse response = deliverNotification(notification.getId(), request);
                results.put(userId, response);
            } catch (Exception e) {
                log.error("Failed to send notification to user {}: {}", userId, e.getMessage(), e);
            }
        }

        return results;
    }

    /**
     * Send notification to a specific FCM token
     */
    private void sendToToken(String token, NotificationRequest request) throws FirebaseMessagingException {
        Message.Builder messageBuilder = Message.builder()
                .setToken(token)
                .setNotification(
                        com.google.firebase.messaging.Notification.builder()
                        .setTitle(request.getTitle())
                        .setBody(request.getBody())
                        .setImage(request.getImageUrl())
                        .build());

        // Add data payload
        if (request.getData() != null && !request.getData().isEmpty()) {
            messageBuilder.putAllData(request.getData());
        }

        // Add click action
        if (request.getClickAction() != null) {
            messageBuilder.putData("click_action", request.getClickAction());
        }

        // Add notification type
        if (request.getNotificationType() != null) {
            messageBuilder.putData("type", request.getNotificationType());
        }

        Message message = messageBuilder.build();

        try {
            String response = firebaseMessaging.send(message);
            log.info("Successfully sent message: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("Error sending message: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Register or update FCM token for a user
     */
    @Transactional
    public NotificationToken registerToken(User user, String token, String deviceType, String deviceInfo) {
        Optional<NotificationToken> existingToken = tokenRepository.findByUserAndToken(user, token);

        if (existingToken.isPresent()) {
            NotificationToken tokenEntity = existingToken.get();
            tokenEntity.setIsActive(true);
            tokenEntity.setLastUsedAt(LocalDateTime.now());
            if (deviceType != null) tokenEntity.setDeviceType(deviceType);
            if (deviceInfo != null) tokenEntity.setDeviceInfo(deviceInfo);
            return tokenRepository.save(tokenEntity);
        } else {
            NotificationToken newToken = new NotificationToken();
            newToken.setUser(user);
            newToken.setToken(token);
            newToken.setDeviceType(deviceType);
            newToken.setDeviceInfo(deviceInfo);
            newToken.setIsActive(true);
            newToken.setLastUsedAt(LocalDateTime.now());
            return tokenRepository.save(newToken);
        }
    }

    /**
     * Unregister token (mark as inactive)
     */
    @Transactional
    public void unregisterToken(User user, String token) {
        tokenRepository.findByUserAndToken(user, token)
                .ifPresent(tokenEntity -> {
                    tokenEntity.setIsActive(false);
                    tokenRepository.save(tokenEntity);
                });
    }
}
