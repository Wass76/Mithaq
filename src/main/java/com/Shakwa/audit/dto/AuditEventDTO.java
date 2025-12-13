package com.Shakwa.audit.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for AuditEvent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventDTO {
    
    private Long id;
    
    private String action;
    
    private String targetType;
    
    private Long targetId;
    
    private Long actorId;
    private String actorName;
    private String actorEmail;
    
    private String status;
    
    private String details;
    
    private String ipAddress;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}

