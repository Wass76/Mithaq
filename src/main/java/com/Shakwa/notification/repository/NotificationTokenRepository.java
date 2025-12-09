package com.Shakwa.notification.repository;

import com.Shakwa.notification.entity.NotificationToken;
import com.Shakwa.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTokenRepository extends JpaRepository<NotificationToken, Long> {

    /**
     * Find all active tokens for a user
     */
    List<NotificationToken> findByUserAndIsActiveTrue(User user);

    /**
     * Find token by user and token string
     */
    Optional<NotificationToken> findByUserAndToken(User user, String token);

    /**
     * Find all active tokens for a user ID
     */
    @Query("SELECT nt FROM NotificationToken nt WHERE nt.user.id = :userId AND nt.isActive = true")
    List<NotificationToken> findActiveTokensByUserId(@Param("userId") Long userId);

    /**
     * Deactivate all tokens for a user
     */
    @Modifying
    @Query("UPDATE NotificationToken nt SET nt.isActive = false WHERE nt.user = :user")
    void deactivateAllTokensByUser(@Param("user") User user);

    /**
     * Update last used timestamp
     */
    @Modifying
    @Query("UPDATE NotificationToken nt SET nt.lastUsedAt = :timestamp WHERE nt.id = :id")
    void updateLastUsedAt(@Param("id") Long id, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Delete old inactive tokens (cleanup)
     */
    @Modifying
    @Query("DELETE FROM NotificationToken nt WHERE nt.isActive = false AND nt.lastUsedAt < :cutoffDate")
    void deleteOldInactiveTokens(@Param("cutoffDate") LocalDateTime cutoffDate);
}
