package com.Shakwa.utils.Aspect;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.Shakwa.utils.annotation.Audited;
import com.Shakwa.audit.service.AuditService;
import com.Shakwa.user.entity.BaseUser;
import com.Shakwa.user.entity.User;
import com.Shakwa.user.entity.Citizen;
import com.Shakwa.user.entity.Employee;
import com.Shakwa.user.repository.UserRepository;
import com.Shakwa.user.repository.CitizenRepo;
import com.Shakwa.user.repository.EmployeeRepository;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Aspect for automatic audit event recording
 * Automatically records audit events for methods annotated with @Audited
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditAspect.class);
    
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final CitizenRepo citizenRepo;
    private final EmployeeRepository employeeRepository;
    
    public AuditAspect(AuditService auditService,
                      UserRepository userRepository,
                      CitizenRepo citizenRepo,
                      EmployeeRepository employeeRepository) {
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.citizenRepo = citizenRepo;
        this.employeeRepository = employeeRepository;
    }

    @AfterReturning(
            pointcut = "@annotation(audited)",
            returning = "result"
    )
    public void auditSuccess(JoinPoint joinPoint, Audited audited, Object result) {
        try {
            String action = audited.action();
            String targetType = audited.targetType().isEmpty() ? 
                inferTargetType(joinPoint) : audited.targetType();
            Long targetId = extractTargetId(result, joinPoint.getArgs());
            String status = "SUCCESS";
            Map<String, Object> details = buildDetails(joinPoint, result, audited.includeArgs());
            
            Long actorId = getCurrentUserId();
            String ipAddress = getClientIpAddress();

            // Record in audit service
            if (actorId != null) {
                auditService.record(action, targetType, targetId, actorId, status, details, ipAddress);
            } else {
                logger.warn("Cannot record audit event: no authenticated user found");
            }
            
        } catch (Exception e) {
            // Don't break the main flow if audit fails
            logger.error("Error recording audit event", e);
        }
    }

    @AfterThrowing(
            pointcut = "@annotation(audited)",
            throwing = "exception"
    )
    public void auditFailure(JoinPoint joinPoint, Audited audited, Throwable exception) {
        try {
            String action = audited.action();
            String targetType = audited.targetType().isEmpty() ? 
                inferTargetType(joinPoint) : audited.targetType();
            Long targetId = extractTargetId(null, joinPoint.getArgs());
            String status = "FAILURE";
            Map<String, Object> details = buildDetails(joinPoint, null, audited.includeArgs());
            details.put("error", exception.getClass().getSimpleName());
            details.put("errorMessage", exception.getMessage());
            
            Long actorId = getCurrentUserId();
            String ipAddress = getClientIpAddress();

            // Record in audit service
            if (actorId != null) {
                auditService.record(action, targetType, targetId, actorId, status, details, ipAddress);
            } else {
                logger.warn("Cannot record audit event: no authenticated user found");
            }
            
        } catch (Exception e) {
            // Don't break the main flow if audit fails
            logger.error("Error recording audit event", e);
        }
    }

    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getPrincipal())) {
                
                String email = authentication.getName();
                
                // Try User (Platform Admins) first
                BaseUser user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    return user.getId();
                }
                
                // Try Citizen
                user = citizenRepo.findByEmail(email).orElse(null);
                if (user != null) {
                    return user.getId();
                }
                
                // Try Employee
                user = employeeRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    return user.getId();
                }
            }
        } catch (Exception e) {
            logger.debug("Could not get current user ID: {}", e.getMessage());
        }
        return null;
    }

    private String inferTargetType(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        // Remove "Service" or "Controller" suffix
        if (className.endsWith("Service")) {
            return className.substring(0, className.length() - 7).toUpperCase();
        }
        if (className.endsWith("Controller")) {
            return className.substring(0, className.length() - 10).toUpperCase();
        }
        return className.toUpperCase();
    }

    private Long extractTargetId(Object result, Object[] args) {
        // Try to extract ID from result (DTO or Entity)
        if (result != null) {
            try {
                // Try getId() method
                java.lang.reflect.Method getIdMethod = result.getClass().getMethod("getId");
                Object id = getIdMethod.invoke(result);
                if (id instanceof Long) {
                    return (Long) id;
                }
            } catch (NoSuchMethodException e) {
                // Try alternative methods for DTOs
                try {
                    // Try getUserId() for user-related DTOs
                    java.lang.reflect.Method getUserIdMethod = result.getClass().getMethod("getUserId");
                    Object id = getUserIdMethod.invoke(result);
                    if (id instanceof Long) {
                        return (Long) id;
                    }
                } catch (Exception ex) {
                    // Try getCitizenId() or getEmployeeId()
                    try {
                        java.lang.reflect.Method getCitizenIdMethod = result.getClass().getMethod("getCitizenId");
                        Object id = getCitizenIdMethod.invoke(result);
                        if (id instanceof Long) {
                            return (Long) id;
                        }
                    } catch (Exception ex2) {
                        try {
                            java.lang.reflect.Method getEmployeeIdMethod = result.getClass().getMethod("getEmployeeId");
                            Object id = getEmployeeIdMethod.invoke(result);
                            if (id instanceof Long) {
                                return (Long) id;
                            }
                        } catch (Exception ex3) {
                            // Ignore
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        
        // Try to extract ID from arguments
        if (args != null && args.length > 0) {
            // Check first argument (usually the ID parameter)
            Object firstArg = args[0];
            if (firstArg instanceof Long) {
                return (Long) firstArg;
            }
            
            // Check second argument if first is not ID (e.g., updateUser(Long id, UserRequestDTO dto))
            if (args.length > 1 && args[0] instanceof Long) {
                return (Long) args[0];
            }
        }
        
        return null;
    }

    private Map<String, Object> buildDetails(JoinPoint joinPoint, Object result, boolean includeArgs) {
        Map<String, Object> details = new HashMap<>();
        details.put("method", joinPoint.getSignature().toShortString());
        
        if (includeArgs && joinPoint.getArgs() != null) {
            // Sanitize arguments to avoid logging sensitive data
            Object[] sanitizedArgs = Arrays.stream(joinPoint.getArgs())
                .map(this::sanitizeSensitiveData)
                .toArray();
            details.put("arguments", sanitizedArgs);
        }
        
        if (result != null) {
            details.put("resultType", result.getClass().getSimpleName());
        }
        
        return details;
    }

    private Object sanitizeSensitiveData(Object obj) {
        if (obj == null) return null;
        
        String str = obj.toString().toLowerCase();
        if (str.contains("password") || str.contains("token") || 
            str.contains("secret") || str.contains("key") || 
            str.contains("jwt") || str.contains("auth")) {
            return "[SENSITIVE_DATA]";
        }
        
        return obj;
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            // Ignore
        }
        return "unknown";
    }
}

