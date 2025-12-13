package com.Shakwa.notification.repository;

import com.Shakwa.notification.entity.NotificationToken;
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
     * Find all active tokens for a user by ID and type
     */
    @Query("SELECT nt FROM NotificationToken nt WHERE nt.userId = :userId AND nt.userType = :userType AND nt.isActive = true")
    List<NotificationToken> findActiveTokensByUserIdAndType(@Param("userId") Long userId, @Param("userType") String userType);

    /**
     * Find token by user ID, type, and token string
     */
    Optional<NotificationToken> findByUserIdAndUserTypeAndToken(Long userId, String userType, String token);

    /**
     * Deactivate all tokens for a user
     */
    @Modifying
    @Query("UPDATE NotificationToken nt SET nt.isActive = false WHERE nt.userId = :userId AND nt.userType = :userType")
    void deactivateAllTokensByUser(@Param("userId") Long userId, @Param("userType") String userType);

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
