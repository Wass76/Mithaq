package com.Shakwa.report.dto;

import java.time.LocalDate;

import com.Shakwa.user.Enum.GovernmentAgencyType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Complaint Status Report
 * Shows count of complaints by status for a specific agency and time period
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintStatusReportDTO {
    
    /**
     * Total number of complaints
     */
    private Long totalComplaints;
    
    /**
     * Number of resolved complaints
     */
    private Long resolvedCount;
    
    /**
     * Number of in-progress complaints
     */
    private Long inProgressCount;
    
    /**
     * Number of pending (new) complaints
     */
    private Long pendingCount;
    
    /**
     * Number of rejected complaints
     */
    private Long rejectedCount;
    
    /**
     * Number of closed complaints
     */
    private Long closedCount;
    
    /**
     * Government agency filter (null = all agencies)
     */
    private GovernmentAgencyType agency;
    
    /**
     * Start date of the report period
     */
    private LocalDate fromDate;
    
    /**
     * End date of the report period
     */
    private LocalDate toDate;
}

