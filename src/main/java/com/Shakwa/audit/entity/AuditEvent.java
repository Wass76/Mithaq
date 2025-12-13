package com.Shakwa.audit.entity;

import com.Shakwa.utils.entity.AuditedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Audit Event Entity
 * Records all system-wide operations for security and compliance auditing
 * 
 * Note: This is separate from ComplaintHistory which tracks complaint-specific changes
 * with detailed field-level information. AuditEvent is for general system operations.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "audit_events", indexes = {
    @Index(name = "idx_audit_events_actor", columnList = "actor_id,actor_type"),
    @Index(name = "idx_audit_events_action", columnList = "action"),
    @Index(name = "idx_audit_events_target", columnList = "target_type,target_id"),
    @Index(name = "idx_audit_events_created_at", columnList = "created_at DESC")
})
@NoArgsConstructor
@AllArgsConstructor
@SequenceGenerator(name = "audit_event_seq", sequenceName = "audit_event_id_seq", allocationSize = 1)
public class AuditEvent extends AuditedEntity {
    
    @Override
    protected String getSequenceName() {
        return "audit_event_id_seq";
    }
    
    /**
     * Action performed (e.g., CREATE_COMPLAINT, UPDATE_USER, LOGIN, etc.)
     */
    @Column(nullable = false, length = 100)
    private String action;
    
    /**
     * Type of target entity (e.g., COMPLAINT, USER, EMPLOYEE, CITIZEN, etc.)
     */
    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;
    
    /**
     * ID of the target entity
     */
    @Column(name = "target_id")
    private Long targetId;
    
    /**
     * ID of the user who performed the action (can be User, Citizen, or Employee)
     */
    @Column(name = "actor_id", nullable = false)
    private Long actorId;
    
    /**
     * Type of the actor (USER, CITIZEN, EMPLOYEE)
     */
    @Column(name = "actor_type", nullable = false, length = 20)
    private String actorType;
    
    /**
     * Status of the action (SUCCESS, FAILURE)
     */
    @Column(nullable = false, length = 20)
    private String status;
    
    /**
     * Additional details in JSON format
     */
    @Column(columnDefinition = "TEXT")
    private String details;
    
    /**
     * IP address of the client who performed the action
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    // createdAt is inherited from AuditedEntity
}

