package com.Shakwa.audit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.Shakwa.audit.entity.AuditEvent;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long>, JpaSpecificationExecutor<AuditEvent> {
    
    /**
     * Find audit events by actor (user who performed the action)
     */
    Page<AuditEvent> findByActorId(Long actorId, Pageable pageable);
    
    /**
     * Find audit events by actor ID and type
     */
    Page<AuditEvent> findByActorIdAndActorType(Long actorId, String actorType, Pageable pageable);
    
    /**
     * Find audit events by action
     */
    Page<AuditEvent> findByAction(String action, Pageable pageable);
    
    /**
     * Find audit events by target type and target ID
     */
    Page<AuditEvent> findByTargetTypeAndTargetId(String targetType, Long targetId, Pageable pageable);
    
    /**
     * Find audit events by target type
     */
    Page<AuditEvent> findByTargetType(String targetType, Pageable pageable);
    
    /**
     * Find audit events by status
     */
    Page<AuditEvent> findByStatus(String status, Pageable pageable);
    
    /**
     * Count audit events by action and status
     */
    long countByActionAndStatus(String action, String status);
    
    /**
     * Count audit events by target type
     */
    long countByTargetType(String targetType);
}

