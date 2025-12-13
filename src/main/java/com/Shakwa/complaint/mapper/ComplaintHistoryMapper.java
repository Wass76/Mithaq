package com.Shakwa.complaint.mapper;

import org.springframework.stereotype.Component;

import com.Shakwa.complaint.dto.ComplaintHistoryDTO;
import com.Shakwa.complaint.entity.ComplaintHistory;

/**
 * Mapper لتحويل ComplaintHistory entity إلى DTO
 */
@Component
public class ComplaintHistoryMapper {
    
    public ComplaintHistoryDTO toDTO(ComplaintHistory history) {
        if (history == null) return null;
        
        ComplaintHistoryDTO dto = ComplaintHistoryDTO.builder()
                .id(history.getId())
                .actionType(history.getActionType())
                .fieldChanged(history.getFieldChanged())
                .oldValue(history.getOldValue())
                .newValue(history.getNewValue())
                .metadata(history.getMetadata())
                .actionDescription(history.getActionDescription())
                .createdAt(history.getCreatedAt())
                .actorId(history.getActorId())
                .actorName(history.getActorName())
                .actorEmail(history.getActorEmail())
                .build();
        
        return dto;
    }
}

