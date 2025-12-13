package com.Shakwa.audit.mapper;

import org.springframework.stereotype.Component;

import com.Shakwa.audit.dto.AuditEventDTO;
import com.Shakwa.audit.entity.AuditEvent;
import com.Shakwa.user.entity.BaseUser;
import com.Shakwa.user.entity.Citizen;
import com.Shakwa.user.entity.Employee;
import com.Shakwa.user.repository.UserRepository;
import com.Shakwa.user.repository.CitizenRepo;
import com.Shakwa.user.repository.EmployeeRepository;

/**
 * Mapper for AuditEvent entity to DTO
 */
@Component
public class AuditEventMapper {
    
    private final UserRepository userRepository;
    private final CitizenRepo citizenRepo;
    private final EmployeeRepository employeeRepository;
    
    public AuditEventMapper(UserRepository userRepository,
                           CitizenRepo citizenRepo,
                           EmployeeRepository employeeRepository) {
        this.userRepository = userRepository;
        this.citizenRepo = citizenRepo;
        this.employeeRepository = employeeRepository;
    }
    
    public AuditEventDTO toDTO(AuditEvent event) {
        if (event == null) {
            return null;
        }
        
        AuditEventDTO dto = AuditEventDTO.builder()
            .id(event.getId())
            .action(event.getAction())
            .targetType(event.getTargetType())
            .targetId(event.getTargetId())
            .status(event.getStatus())
            .details(event.getDetails())
            .ipAddress(event.getIpAddress())
            .createdAt(event.getCreatedAt())
            .build();
        
        // Map actor information if available
        if (event.getActorId() != null) {
            dto.setActorId(event.getActorId());
            
            // Fetch actor based on actor type
            BaseUser actor = null;
            if ("USER".equals(event.getActorType())) {
                actor = userRepository.findById(event.getActorId()).orElse(null);
            } else if ("CITIZEN".equals(event.getActorType())) {
                actor = citizenRepo.findById(event.getActorId()).orElse(null);
            } else if ("EMPLOYEE".equals(event.getActorType())) {
                actor = employeeRepository.findById(event.getActorId()).orElse(null);
            }
            
            if (actor != null) {
                if (actor instanceof Citizen citizen) {
                    dto.setActorName(citizen.getFirstName() + " " + citizen.getLastName());
                    dto.setActorEmail(citizen.getEmail());
                } else if (actor instanceof Employee employee) {
                    dto.setActorName(employee.getFirstName() + " " + employee.getLastName());
                    dto.setActorEmail(employee.getEmail());
                } else {
                    dto.setActorName(actor.getFirstName() + " " + actor.getLastName());
                    dto.setActorEmail(actor.getEmail());
                }
            }
        }
        
        return dto;
    }
}

