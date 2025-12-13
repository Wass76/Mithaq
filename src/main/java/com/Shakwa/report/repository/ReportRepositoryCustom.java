package com.Shakwa.report.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.Shakwa.complaint.Enum.ComplaintStatus;
import com.Shakwa.complaint.entity.Complaint;
import com.Shakwa.user.Enum.GovernmentAgencyType;

/**
 * Custom repository interface for report queries.
 * Uses dynamic query building to avoid PostgreSQL parameter type inference issues.
 */
public interface ReportRepositoryCustom {
    
    /**
     * Count complaints by status for a specific agency and date range
     */
    List<Object[]> countComplaintsByStatus(
        GovernmentAgencyType agency,
        LocalDateTime fromDate,
        LocalDateTime toDate
    );
    
    /**
     * Count complaints by type for a specific agency and date range
     */
    List<Object[]> countComplaintsByType(
        GovernmentAgencyType agency,
        LocalDateTime fromDate,
        LocalDateTime toDate
    );
    
    /**
     * Get total count of complaints for a specific agency and date range
     */
    Long countTotalComplaints(
        GovernmentAgencyType agency,
        LocalDateTime fromDate,
        LocalDateTime toDate
    );
    
    /**
     * Get count of complaints by specific status for a specific agency and date range
     */
    Long countComplaintsByStatusValue(
        ComplaintStatus status,
        GovernmentAgencyType agency,
        LocalDateTime fromDate,
        LocalDateTime toDate
    );
    
    /**
     * Get all resolved complaints for a specific agency and date range
     */
    List<Complaint> findResolvedComplaints(
        ComplaintStatus status,
        GovernmentAgencyType agency,
        LocalDateTime fromDate,
        LocalDateTime toDate
    );
}
