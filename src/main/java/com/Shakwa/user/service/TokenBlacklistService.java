package com.Shakwa.user.service;

import com.Shakwa.config.JwtService;
import com.Shakwa.user.entity.TokenBlacklist;
import com.Shakwa.user.repository.TokenBlacklistRepository;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JwtService jwtService;

    /**
     * Blacklists a JWT token to invalidate it
     * @param token The JWT token to blacklist
     * @param userEmail The email of the user who owns the token
     */
    @Transactional
    public void blacklistToken(String token, String userEmail) {
        // Extract expiry date from token
        Claims claims = jwtService.extractAllClaims(token);
        Date expiration = claims.getExpiration();
        LocalDateTime expiryDate = expiration.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        TokenBlacklist blacklistedToken = TokenBlacklist.builder()
                .token(token)
                .userEmail(userEmail)
                .blacklistedAt(LocalDateTime.now())
                .expiryDate(expiryDate)
                .build();

        tokenBlacklistRepository.save(blacklistedToken);
        log.info("Token blacklisted for user: {}", userEmail);
    }

    /**
     * Checks if a token is blacklisted
     * @param token The JWT token to check
     * @return true if the token is blacklisted, false otherwise
     */
    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklistRepository.existsByToken(token);
    }

    /**
     * Scheduled task to clean up expired tokens from the blacklist
     * Runs every hour
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void cleanupExpiredTokens() {
        tokenBlacklistRepository.deleteExpiredTokens(LocalDateTime.now());
        log.debug("Cleaned up expired tokens from blacklist");
    }
}
