package com.Shakwa.report.dto;

import java.time.LocalDate;

import com.Shakwa.user.Enum.GovernmentAgencyType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Average Resolution Time Report
 * Shows average time taken to resolve complaints
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AverageResolutionTimeReportDTO {
    
    /**
     * Average resolution time in days
     */
    private Double averageDays;
    
    /**
     * Average resolution time in hours
     */
    private Double averageHours;
    
    /**
     * Minimum resolution time in days
     */
    private Long minResolutionDays;
    
    /**
     * Maximum resolution time in days
     */
    private Long maxResolutionDays;
    
    /**
     * Total number of resolved complaints used in calculation
     */
    private Long totalResolvedComplaints;
    
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

