package com.Shakwa.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.Shakwa.user.entity.BaseUser;
import com.Shakwa.user.entity.User;
import com.Shakwa.user.entity.Citizen;
import com.Shakwa.user.entity.Employee;
import com.Shakwa.user.repository.CitizenRepo;
import com.Shakwa.user.repository.EmployeeRepository;
import com.Shakwa.user.repository.UserRepository;
import com.Shakwa.utils.exception.RequestNotValidException;
import com.Shakwa.utils.exception.ResourceNotFoundException;
import com.Shakwa.utils.exception.UnAuthorizedException;
import com.Shakwa.utils.annotation.Audited;

/**
 * Service for password management
 * Handles password change and reset operations
 */
@Service
@Transactional
public class PasswordService extends BaseSecurityService {
    
    private static final Logger logger = LoggerFactory.getLogger(PasswordService.class);
    
    private final PasswordEncoder passwordEncoder;
    
    public PasswordService(UserRepository userRepository,
                          CitizenRepo citizenRepo,
                          EmployeeRepository employeeRepository,
                          PasswordEncoder passwordEncoder) {
        super(userRepository, citizenRepo, employeeRepository);
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * Change password for the current authenticated user
     * 
     * @param oldPassword Current password
     * @param newPassword New password
     */
    @Audited(action = "CHANGE_PASSWORD", targetType = "USER", includeArgs = false)
    public void changePassword(String oldPassword, String newPassword) {
        if (!StringUtils.hasText(oldPassword)) {
            throw new RequestNotValidException("Current password is required");
        }
        if (!StringUtils.hasText(newPassword)) {
            throw new RequestNotValidException("New password is required");
        }
        if (newPassword.length() < 8) {
            throw new RequestNotValidException("New password must be at least 8 characters long");
        }
        
        BaseUser currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new UnAuthorizedException("User not authenticated");
        }
        
        // Verify old password
        if (!passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
            throw new RequestNotValidException("Current password is incorrect");
        }
        
        // Update password
        currentUser.setPassword(passwordEncoder.encode(newPassword));
        // Save based on user type using protected fields from BaseSecurityService
        if (currentUser instanceof User user) {
            userRepository.save(user);
        } else if (currentUser instanceof Citizen citizen) {
            citizenRepo.save(citizen);
        } else if (currentUser instanceof Employee employee) {
            employeeRepository.save(employee);
        }
        
        logger.info("Password changed successfully for user: {}", currentUser.getEmail());
    }
    
    /**
     * Reset password by admin (for any user)
     * Only platform admins can use this method
     * 
     * @param userId ID of the user whose password will be reset
     * @param newPassword New password
     */
    @Audited(action = "RESET_PASSWORD", targetType = "USER", includeArgs = false)
    public void resetPassword(Long userId, String newPassword) {
        if (!isAdmin()) {
            throw new UnAuthorizedException("Only platform admins can reset passwords");
        }
        
        if (!StringUtils.hasText(newPassword)) {
            throw new RequestNotValidException("New password is required");
        }
        if (newPassword.length() < 8) {
            throw new RequestNotValidException("New password must be at least 8 characters long");
        }
        
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        logger.info("Password reset by admin for user: {} (ID: {})", user.getEmail(), userId);
    }
    
    /**
     * Request password reset (future implementation - sends email with reset link)
     * Currently just logs the request
     * 
     * @param email Email of the user requesting password reset
     */
    public void requestPasswordReset(String email) {
        if (!StringUtils.hasText(email)) {
            throw new RequestNotValidException("Email is required");
        }
        
        userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        // TODO: Generate reset token and send email
        // For now, just log the request
        logger.info("Password reset requested for email: {}", email);
        
        // In future: Generate reset token, save it, and send email
        // resetTokenService.generateAndSendResetToken(user);
    }
}

