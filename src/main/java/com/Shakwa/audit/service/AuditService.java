package com.Shakwa.audit.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Shakwa.audit.dto.AuditEventDTO;
import com.Shakwa.audit.entity.AuditEvent;
import com.Shakwa.audit.mapper.AuditEventMapper;
import com.Shakwa.audit.repository.AuditEventRepository;
import com.Shakwa.audit.repository.AuditEventSpecification;
import com.Shakwa.user.dto.PaginationDTO;
import com.Shakwa.user.entity.BaseUser;
import com.Shakwa.user.repository.UserRepository;
import com.Shakwa.user.repository.CitizenRepo;
import com.Shakwa.user.repository.EmployeeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for managing audit events
 * Records all system-wide operations for security and compliance
 */
@Service
@Transactional
public class AuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    private final AuditEventRepository auditEventRepository;
    private final AuditEventMapper auditEventMapper;
    private final UserRepository userRepository;
    private final CitizenRepo citizenRepo;
    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper;
    
    public AuditService(AuditEventRepository auditEventRepository,
                       AuditEventMapper auditEventMapper,
                       UserRepository userRepository,
                       CitizenRepo citizenRepo,
                       EmployeeRepository employeeRepository,
                       ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.auditEventMapper = auditEventMapper;
        this.userRepository = userRepository;
        this.citizenRepo = citizenRepo;
        this.employeeRepository = employeeRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Record an audit event
     * 
     * @param action Action performed (e.g., CREATE_COMPLAINT, UPDATE_USER)
     * @param targetType Type of target entity (e.g., COMPLAINT, USER)
     * @param targetId ID of target entity
     * @param actorId ID of user who performed the action
     * @param status Status (SUCCESS, FAILURE)
     * @param details Additional details as Map
     * @param ipAddress IP address of the client
     */
    public void record(String action, String targetType, Long targetId, 
                      Long actorId, String status, Map<String, Object> details, 
                      String ipAddress) {
        try {
            // Determine actor type by trying to find in each repository
            String actorType = null;
            BaseUser actor = userRepository.findById(actorId).orElse(null);
            if (actor != null) {
                actorType = "USER";
            } else {
                actor = citizenRepo.findById(actorId).orElse(null);
                if (actor != null) {
                    actorType = "CITIZEN";
                } else {
                    actor = employeeRepository.findById(actorId).orElse(null);
                    if (actor != null) {
                        actorType = "EMPLOYEE";
                    } else {
                        logger.warn("Cannot record audit event: User not found with ID: {}", actorId);
                        return;
                    }
                }
            }
            
            AuditEvent event = new AuditEvent();
            event.setAction(action);
            event.setTargetType(targetType);
            event.setTargetId(targetId);
            event.setActorId(actorId);
            event.setActorType(actorType);
            event.setStatus(status);
            event.setIpAddress(ipAddress);
            
            // Convert details map to JSON string
            if (details != null && !details.isEmpty()) {
                try {
                    event.setDetails(objectMapper.writeValueAsString(details));
                } catch (JsonProcessingException e) {
                    logger.warn("Failed to serialize audit details to JSON: {}", e.getMessage());
                    event.setDetails(details.toString());
                }
            }
            
            auditEventRepository.save(event);
            logger.debug("Audit event recorded: {} | {}[{}] | Actor: {} | Status: {}", 
                action, targetType, targetId, actorId, status);
            
        } catch (Exception e) {
            // Don't throw exception - audit logging should not break the main flow
            logger.error("Failed to record audit event: {} | {}[{}] | Error: {}", 
                action, targetType, targetId, e.getMessage(), e);
        }
    }
    
    /**
     * Record an audit event (simplified version without IP)
     */
    public void record(String action, String targetType, Long targetId, 
                      Long actorId, String status, Map<String, Object> details) {
        record(action, targetType, targetId, actorId, status, details, null);
    }
    
    /**
     * Get audit logs with filters and pagination.
     * Uses JPA Specifications to avoid PostgreSQL parameter type inference issues.
     */
    @Transactional(readOnly = true)
    public PaginationDTO<AuditEventDTO> getAuditLogs(
        Long userId, String action, String targetType, Long targetId,
        String status, LocalDateTime fromDate, LocalDateTime toDate,
        int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        
        // Use Specification for dynamic query building
        // This avoids the PostgreSQL issue with null parameter type inference
        Page<AuditEvent> eventPage = auditEventRepository.findAll(
            AuditEventSpecification.withFilters(
                userId, action, targetType, targetId, status, fromDate, toDate
            ),
            pageable
        );
        
        Page<AuditEventDTO> dtoPage = eventPage.map(auditEventMapper::toDTO);
        return PaginationDTO.of(dtoPage);
    }
    
    /**
     * Get audit events by actor
     */
    @Transactional(readOnly = true)
    public PaginationDTO<AuditEventDTO> getAuditLogsByActor(Long actorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditEvent> eventPage = auditEventRepository.findByActorId(actorId, pageable);
        Page<AuditEventDTO> dtoPage = eventPage.map(auditEventMapper::toDTO);
        return PaginationDTO.of(dtoPage);
    }
    
    /**
     * Get audit events by action
     */
    @Transactional(readOnly = true)
    public PaginationDTO<AuditEventDTO> getAuditLogsByAction(String action, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditEvent> eventPage = auditEventRepository.findByAction(action, pageable);
        Page<AuditEventDTO> dtoPage = eventPage.map(auditEventMapper::toDTO);
        return PaginationDTO.of(dtoPage);
    }
    
    /**
     * Get audit events by target
     */
    @Transactional(readOnly = true)
    public PaginationDTO<AuditEventDTO> getAuditLogsByTarget(String targetType, Long targetId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditEvent> eventPage = auditEventRepository.findByTargetTypeAndTargetId(targetType, targetId, pageable);
        Page<AuditEventDTO> dtoPage = eventPage.map(auditEventMapper::toDTO);
        return PaginationDTO.of(dtoPage);
    }
}

