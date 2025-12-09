package com.Shakwa.notification.repository;

import com.Shakwa.notification.entity.Notification;
import com.Shakwa.user.entity.BaseUser;
import com.Shakwa.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find all notifications for a user
     */
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * Find unread notifications for a user
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.readAt IS NULL ORDER BY n.createdAt DESC")
    List<Notification> findUnreadNotificationsByUser(@Param("user") User user);

    /**
     * Count unread notifications for a user
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :user AND n.readAt IS NULL")
    Long countUnreadNotificationsByUser(@Param("user") User user);

    /**
     * Mark notification as read
     */
    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :readAt WHERE n.id = :id")
    void markAsRead(@Param("id") Long id, @Param("readAt") LocalDateTime readAt);

    /**
     * Mark all notifications as read for a user
     */
    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :readAt WHERE n.user = :user AND n.readAt IS NULL")
    void markAllAsReadByUser(@Param("user") User user, @Param("readAt") LocalDateTime readAt);

    /**
     * Find notifications by status
     */
    List<Notification> findByStatus(Notification.NotificationStatus status);

    /**
     * Delete old notifications (cleanup)
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    void deleteOldNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);
}
