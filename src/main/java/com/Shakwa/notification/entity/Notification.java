package com.Shakwa.notification.entity;

import com.Shakwa.user.entity.BaseUser;
import com.Shakwa.utils.entity.AuditedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity to store notification records
 * Tracks all notifications sent to users
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "notifications")
@NoArgsConstructor
@AllArgsConstructor
@SequenceGenerator(name = "notification_seq", sequenceName = "notification_id_seq", allocationSize = 1)
public class Notification extends AuditedEntity {

    @Override
    protected String getSequenceName() {
        return "notification_id_seq";
    }

    /**
     * ID of the user to receive the notification
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * Type of user (USER, CITIZEN, EMPLOYEE)
     */
    @Column(name = "user_type", nullable = false)
    private String userType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "body", nullable = false, length = 1000)
    private String body;

    @Column(name = "data", columnDefinition = "TEXT")
    private String data; // JSON string for additional data

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "notification_type", length = 100)
    private String notificationType; // e.g., "complaint_update", "system_alert"

    public enum NotificationStatus {
        PENDING,
        SENT,
        DELIVERED,
        FAILED,
        READ
    }
}
