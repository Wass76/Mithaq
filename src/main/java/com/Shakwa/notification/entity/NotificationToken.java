package com.Shakwa.notification.entity;

import com.Shakwa.user.entity.BaseUser;
import com.Shakwa.utils.entity.AuditedEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity to store FCM (Firebase Cloud Messaging) tokens for users
 * Each user can have multiple tokens (multiple devices)
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "notification_tokens", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "user_type", "token"})
})
@NoArgsConstructor
@AllArgsConstructor
@SequenceGenerator(name = "notification_token_seq", sequenceName = "notification_token_id_seq", allocationSize = 1)
public class NotificationToken extends AuditedEntity {

    @Override
    protected String getSequenceName() {
        return "notification_token_id_seq";
    }

    /**
     * ID of the user who owns this token
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * Type of user (USER, CITIZEN, EMPLOYEE)
     */
    @Column(name = "user_type", nullable = false)
    private String userType;

    @Column(name = "token", nullable = false, length = 1000)
    private String token;

    @Column(name = "device_type", length = 50)
    private String deviceType; // e.g., "web", "android", "ios"

    @Column(name = "device_info", length = 500)
    private String deviceInfo; // Additional device information

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
