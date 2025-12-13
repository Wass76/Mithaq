package com.Shakwa.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Shakwa.user.service.TokenBlacklistService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for authentication operations (logout)")
@SecurityRequirement(name = "BearerAuth")
@CrossOrigin("*")
@Slf4j
public class AuthController {

    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Logs out the current user by invalidating their JWT token. Works for all user types: Admin, Employee, and Citizen."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout successful"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "No valid token provided"));
        }

        String token = authHeader.substring(7);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated()) {
            String userEmail = authentication.getName();
            tokenBlacklistService.blacklistToken(token, userEmail);
            SecurityContextHolder.clearContext();
            log.info("User {} logged out successfully", userEmail);
        }

        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }
}
