package com.Shakwa.report.dto;

import java.time.LocalDate;
import java.util.List;

import com.Shakwa.complaint.Enum.ComplaintType;
import com.Shakwa.user.Enum.GovernmentAgencyType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Complaint Type Distribution Report
 * Shows distribution of complaints by type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintTypeDistributionDTO {
    
    /**
     * List of complaint types with their counts and percentages
     */
    private List<TypeCount> distribution;
    
    /**
     * Total number of complaints
     */
    private Long totalComplaints;
    
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
    
    /**
     * Inner class for type count statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeCount {
        private ComplaintType type;
        private Long count;
        private Double percentage;
    }
}

