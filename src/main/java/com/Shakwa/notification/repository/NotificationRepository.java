package com.Shakwa.notification.repository;

import com.Shakwa.notification.entity.Notification;
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
     * Find all notifications for a user by ID and type
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.userType = :userType ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdAndUserTypeOrderByCreatedAtDesc(
            @Param("userId") Long userId, 
            @Param("userType") String userType, 
            Pageable pageable);

    /**
     * Find unread notifications for a user by ID and type
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.userType = :userType AND n.readAt IS NULL ORDER BY n.createdAt DESC")
    List<Notification> findUnreadNotificationsByUserIdAndType(
            @Param("userId") Long userId, 
            @Param("userType") String userType);

    /**
     * Count unread notifications for a user by ID and type
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.userType = :userType AND n.readAt IS NULL")
    Long countUnreadNotificationsByUserIdAndType(
            @Param("userId") Long userId, 
            @Param("userType") String userType);

    /**
     * Mark notification as read
     */
    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :readAt WHERE n.id = :id")
    void markAsRead(@Param("id") Long id, @Param("readAt") LocalDateTime readAt);

    /**
     * Mark all notifications as read for a user by ID and type
     */
    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :readAt WHERE n.userId = :userId AND n.userType = :userType AND n.readAt IS NULL")
    void markAllAsReadByUserIdAndType(
            @Param("userId") Long userId, 
            @Param("userType") String userType,
            @Param("readAt") LocalDateTime readAt);

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
