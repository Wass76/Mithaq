package com.Shakwa.report.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Shakwa.complaint.Enum.ComplaintStatus;
import com.Shakwa.complaint.Enum.ComplaintType;
import com.Shakwa.complaint.Enum.HistoryActionType;
import com.Shakwa.complaint.entity.Complaint;
import com.Shakwa.complaint.entity.ComplaintHistory;
import com.Shakwa.complaint.repository.ComplaintHistoryRepository;
import com.Shakwa.report.dto.AverageResolutionTimeReportDTO;
import com.Shakwa.report.dto.ComplaintStatusReportDTO;
import com.Shakwa.report.dto.ComplaintTypeDistributionDTO;
import com.Shakwa.report.dto.ComplaintTypeDistributionDTO.TypeCount;
import com.Shakwa.report.repository.ReportRepository;
import com.Shakwa.user.Enum.GovernmentAgencyType;
import com.Shakwa.user.service.BaseSecurityService;
import com.Shakwa.user.repository.CitizenRepo;
import com.Shakwa.user.repository.EmployeeRepository;
import com.Shakwa.user.repository.UserRepository;
import com.Shakwa.utils.exception.UnAuthorizedException;

/**
 * Service for generating reports
 * Provides methods for employee reports (status, resolution time, type distribution)
 */
@Service
@Transactional(readOnly = true)
public class ReportService extends BaseSecurityService {
    
    private final ReportRepository reportRepository;
    private final ComplaintHistoryRepository complaintHistoryRepository;
    
    public ReportService(ReportRepository reportRepository,
                        ComplaintHistoryRepository complaintHistoryRepository,
                        UserRepository userRepository,
                        CitizenRepo citizenRepo,
                        EmployeeRepository employeeRepository) {
        super(userRepository, citizenRepo, employeeRepository);
        this.reportRepository = reportRepository;
        this.complaintHistoryRepository = complaintHistoryRepository;
    }
    
    /**
     * Get complaint status report
     * Shows count of complaints by status for a specific agency and time period
     * 
     * @param agency Government agency (null = all agencies, only for admins)
     * @param fromDate Start date (optional)
     * @param toDate End date (optional)
     * @return ComplaintStatusReportDTO
     */
    public ComplaintStatusReportDTO getComplaintStatusReport(
            GovernmentAgencyType agency, LocalDate fromDate, LocalDate toDate) {
        
        // Validate access: employees can only see their agency's reports
        GovernmentAgencyType userAgency = validateAndGetAgency(agency);
        
        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(23, 59, 59) : null;
        
        // Get counts by status
        List<Object[]> statusCounts = reportRepository.countComplaintsByStatus(
            userAgency, fromDateTime, toDateTime);
        
        // Initialize counts
        long total = 0;
        long resolved = 0;
        long inProgress = 0;
        long pending = 0;
        long rejected = 0;
        long closed = 0;
        
        // Process results
        for (Object[] row : statusCounts) {
            ComplaintStatus status = (ComplaintStatus) row[0];
            Long count = (Long) row[1];
            total += count;
            
            switch (status) {
                case RESOLVED -> resolved = count;
                case IN_PROGRESS -> inProgress = count;
                case PENDING -> pending = count;
                case REJECTED -> rejected = count;
                case CLOSED -> closed = count;
            }
        }
        
        // If no results, get total count
        if (total == 0) {
            total = reportRepository.countTotalComplaints(userAgency, fromDateTime, toDateTime);
        }
        
        return ComplaintStatusReportDTO.builder()
            .totalComplaints(total)
            .resolvedCount(resolved)
            .inProgressCount(inProgress)
            .pendingCount(pending)
            .rejectedCount(rejected)
            .closedCount(closed)
            .agency(userAgency)
            .fromDate(fromDate)
            .toDate(toDate)
            .build();
    }
    
    /**
     * Get average resolution time report
     * Calculates average time taken to resolve complaints using ComplaintHistory
     * 
     * @param agency Government agency (null = all agencies, only for admins)
     * @param fromDate Start date (optional)
     * @param toDate End date (optional)
     * @return AverageResolutionTimeReportDTO
     */
    public AverageResolutionTimeReportDTO getAverageResolutionTimeReport(
            GovernmentAgencyType agency, LocalDate fromDate, LocalDate toDate) {
        
        // Validate access
        GovernmentAgencyType userAgency = validateAndGetAgency(agency);
        
        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(23, 59, 59) : null;
        
        // Get all resolved complaints for the agency and date range
        // Use repository query for better performance
        List<Complaint> resolvedComplaints = reportRepository.findResolvedComplaints(
            ComplaintStatus.RESOLVED,
            userAgency,
            fromDateTime,
            toDateTime
        );
        
        if (resolvedComplaints.isEmpty()) {
            return AverageResolutionTimeReportDTO.builder()
                .averageDays(0.0)
                .averageHours(0.0)
                .minResolutionDays(0L)
                .maxResolutionDays(0L)
                .totalResolvedComplaints(0L)
                .agency(userAgency)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();
        }
        
        // Calculate resolution times using ComplaintHistory
        List<Long> resolutionDays = new ArrayList<>();
        
        for (Complaint complaint : resolvedComplaints) {
            // Find when the complaint was created
            LocalDateTime createdTime = complaint.getCreatedAt();
            
            // Find when status was changed to RESOLVED in ComplaintHistory
            List<ComplaintHistory> histories = complaintHistoryRepository
                .findByComplaintIdOrderByCreatedAtDesc(complaint.getId());
            
            LocalDateTime resolvedTime = null;
            for (ComplaintHistory history : histories) {
                if (history.getActionType() == HistoryActionType.STATUS_CHANGED &&
                    "RESOLVED".equals(history.getNewValue())) {
                    resolvedTime = history.getCreatedAt();
                    break;
                }
            }
            
            // If not found in history, use respondedAt or current time
            if (resolvedTime == null) {
                if (complaint.getRespondedAt() != null) {
                    resolvedTime = complaint.getRespondedAt();
                } else {
                    resolvedTime = complaint.getUpdatedAt() != null ? 
                        complaint.getUpdatedAt() : LocalDateTime.now();
                }
            }
            
            // Calculate days between creation and resolution
            long days = ChronoUnit.DAYS.between(createdTime, resolvedTime);
            resolutionDays.add(days);
        }
        
        // Calculate statistics
        double avgDays = resolutionDays.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        double avgHours = avgDays * 24;
        
        long minDays = resolutionDays.stream()
            .mapToLong(Long::longValue)
            .min()
            .orElse(0L);
        
        long maxDays = resolutionDays.stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0L);
        
        return AverageResolutionTimeReportDTO.builder()
            .averageDays(avgDays)
            .averageHours(avgHours)
            .minResolutionDays(minDays)
            .maxResolutionDays(maxDays)
            .totalResolvedComplaints((long) resolvedComplaints.size())
            .agency(userAgency)
            .fromDate(fromDate)
            .toDate(toDate)
            .build();
    }
    
    /**
     * Get complaint type distribution report
     * Shows distribution of complaints by type
     * 
     * @param agency Government agency (null = all agencies, only for admins)
     * @param fromDate Start date (optional)
     * @param toDate End date (optional)
     * @return ComplaintTypeDistributionDTO
     */
    public ComplaintTypeDistributionDTO getComplaintTypeDistribution(
            GovernmentAgencyType agency, LocalDate fromDate, LocalDate toDate) {
        
        // Validate access
        GovernmentAgencyType userAgency = validateAndGetAgency(agency);
        
        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(23, 59, 59) : null;
        
        // Get counts by type
        List<Object[]> typeCounts = reportRepository.countComplaintsByType(
            userAgency, fromDateTime, toDateTime);
        
        // Get total count
        Long total = reportRepository.countTotalComplaints(userAgency, fromDateTime, toDateTime);
        
        // Build distribution list
        List<TypeCount> distribution = new ArrayList<>();
        
        for (Object[] row : typeCounts) {
            ComplaintType type = (ComplaintType) row[0];
            Long count = (Long) row[1];
            Double percentage = total > 0 ? (count * 100.0 / total) : 0.0;
            
            distribution.add(TypeCount.builder()
                .type(type)
                .count(count)
                .percentage(percentage)
                .build());
        }
        
        return ComplaintTypeDistributionDTO.builder()
            .distribution(distribution)
            .totalComplaints(total)
            .agency(userAgency)
            .fromDate(fromDate)
            .toDate(toDate)
            .build();
    }
    
    /**
     * Validate agency access and return the agency to use
     * Employees can only access their own agency's reports
     * Admins can access any agency or all agencies
     */
    private GovernmentAgencyType validateAndGetAgency(GovernmentAgencyType requestedAgency) {
        // If admin, allow any agency (including null for all)
        if (isAdmin()) {
            return requestedAgency;
        }
        
        // If employee, must use their own agency
        try {
            GovernmentAgencyType userAgency = getCurrentUserGovernmentAgency();
            
            // If requesting specific agency, must match user's agency
            if (requestedAgency != null && !requestedAgency.equals(userAgency)) {
                throw new UnAuthorizedException(
                    "You can only view reports for your own agency: " + userAgency.getLabel());
            }
            
            return userAgency;
        } catch (UnAuthorizedException e) {
            // User is not an employee, check if they're a citizen
            if (isCurrentUserCitizen()) {
                throw new UnAuthorizedException("Citizens cannot access reports");
            }
            throw e;
        }
    }
}

