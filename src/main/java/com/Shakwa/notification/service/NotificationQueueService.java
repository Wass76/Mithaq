package com.Shakwa.notification.service;

import com.Shakwa.notification.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Simple in-memory queue to offload FCM calls to a background thread pool.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationQueueService {

    private final FirebaseNotificationService firebaseNotificationService;
    private final @Qualifier("notificationTaskExecutor") TaskExecutor notificationTaskExecutor;

    private final BlockingQueue<NotificationTask> queue = new LinkedBlockingQueue<>();

    @PostConstruct
    void startConsumer() {
        notificationTaskExecutor.execute(this::consume);
    }

    public void enqueue(Long notificationId, NotificationRequest request) {
        log.info("Queueing notification {} for user {} type {}", notificationId, request.getUserId(), request.getNotificationType());
        queue.offer(new NotificationTask(notificationId, request));
    }

    private void consume() {
        while (true) {
            try {
                NotificationTask task = queue.take();
                firebaseNotificationService.deliverNotification(task.notificationId(), task.request());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception ex) {
                log.error("Notification dispatch failed", ex);
            }
        }
    }

    private record NotificationTask(Long notificationId, NotificationRequest request) { }
}

