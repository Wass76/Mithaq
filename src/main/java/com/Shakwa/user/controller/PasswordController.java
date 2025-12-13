package com.Shakwa.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Shakwa.user.dto.ChangePasswordRequest;
import com.Shakwa.user.dto.PasswordResetRequest;
import com.Shakwa.user.dto.ResetPasswordRequest;
import com.Shakwa.user.service.PasswordService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controller for password management
 */
@RestController
@RequestMapping("api/v1/password")
@Tag(name = "Password Management", description = "APIs for password change and reset")
public class PasswordController {
    
    private final PasswordService passwordService;
    
    public PasswordController(PasswordService passwordService) {
        this.passwordService = passwordService;
    }
    
    @PutMapping("/change")
    @Operation(
        summary = "Change password", 
        description = "Change password for the currently authenticated user. Requires current password."
    )
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "Password change request", required = true)
            @Valid @RequestBody ChangePasswordRequest request) {
        passwordService.changePassword(request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/reset")
    @Operation(
        summary = "Reset password (Admin only)", 
        description = "Reset password for any user. Only platform admins can use this endpoint."
    )
    public ResponseEntity<Void> resetPassword(
            @Parameter(description = "Password reset request", required = true)
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordService.resetPassword(request.getUserId(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/reset-request")
    @Operation(
        summary = "Request password reset", 
        description = "Request password reset email. Currently logs the request (email sending to be implemented)."
    )
    public ResponseEntity<Void> requestPasswordReset(
            @Parameter(description = "Password reset request", required = true)
            @Valid @RequestBody PasswordResetRequest request) {
        passwordService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok().build();
    }
}

