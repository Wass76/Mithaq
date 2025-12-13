package com.Shakwa.admin.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Shakwa.admin.dto.DashboardOverviewDTO;
import com.Shakwa.admin.dto.DashboardOverviewDTO.AgencyComplaintCount;
import com.Shakwa.admin.dto.DashboardOverviewDTO.TypeComplaintCount;
import com.Shakwa.complaint.Enum.ComplaintStatus;
import com.Shakwa.complaint.Enum.HistoryActionType;
import com.Shakwa.complaint.dto.ComplaintDTOResponse;
import com.Shakwa.complaint.entity.Complaint;
import com.Shakwa.complaint.entity.ComplaintHistory;
import com.Shakwa.complaint.mapper.ComplaintMapper;
import com.Shakwa.complaint.repository.ComplaintHistoryRepository;
import com.Shakwa.complaint.repository.ComplaintRepository;
import com.Shakwa.user.service.BaseSecurityService;
import com.Shakwa.user.repository.CitizenRepo;
import com.Shakwa.user.repository.EmployeeRepository;
import com.Shakwa.user.repository.UserRepository;
import com.Shakwa.utils.exception.UnAuthorizedException;

/**
 * Service for Admin Dashboard
 * Provides comprehensive statistics and KPIs for platform admins
 */
@Service
@Transactional(readOnly = true)
public class AdminDashboardService extends BaseSecurityService {
    
    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository complaintHistoryRepository;
    private final ComplaintMapper complaintMapper;
    
    // Default values
    private static final int DEFAULT_OVERDUE_DAYS_THRESHOLD = 30;
    private static final int DEFAULT_TOP_AGENCIES_LIMIT = 5;
    private static final int DEFAULT_TOP_TYPES_LIMIT = 5;
    private static final int DEFAULT_OVERDUE_COMPLAINTS_LIMIT = 10;
    
    public AdminDashboardService(ComplaintRepository complaintRepository,
                                ComplaintHistoryRepository complaintHistoryRepository,
                                ComplaintMapper complaintMapper,
                                UserRepository userRepository,
                                CitizenRepo citizenRepo,
                                EmployeeRepository employeeRepository) {
        super(userRepository, citizenRepo, employeeRepository);
        this.complaintRepository = complaintRepository;
        this.complaintHistoryRepository = complaintHistoryRepository;
        this.complaintMapper = complaintMapper;
    }
    
    /**
     * Get dashboard overview with comprehensive statistics
     * Only accessible to platform admins
     * 
     * @param fromDate Start date (optional, default: 30 days ago)
     * @param toDate End date (optional, default: today)
     * @param overdueDaysThreshold Days threshold for overdue complaints (optional, default: 30)
     * @return DashboardOverviewDTO
     */
    public DashboardOverviewDTO getDashboardOverview(
            LocalDate fromDate, LocalDate toDate, Integer overdueDaysThreshold) {
        
        // Only admins can access dashboard
        if (!isAdmin()) {
            throw new UnAuthorizedException("Only platform admins can access dashboard overview");
        }
        
        // Set default dates if not provided
        if (fromDate == null) {
            fromDate = LocalDate.now().minusDays(30);
        }
        if (toDate == null) {
            toDate = LocalDate.now();
        }
        if (overdueDaysThreshold == null) {
            overdueDaysThreshold = DEFAULT_OVERDUE_DAYS_THRESHOLD;
        }
        
        LocalDateTime fromDateTime = fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate.atTime(23, 59, 59);
        
        // Get all complaints in the date range
        List<Complaint> allComplaints = complaintRepository.findAll().stream()
            .filter(c -> {
                LocalDateTime createdAt = c.getCreatedAt();
                return createdAt != null && 
                       (createdAt.isAfter(fromDateTime) || createdAt.isEqual(fromDateTime)) &&
                       (createdAt.isBefore(toDateTime) || createdAt.isEqual(toDateTime));
            })
            .collect(Collectors.toList());
        
        // Calculate basic statistics
        long total = allComplaints.size();
        long resolved = allComplaints.stream()
            .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED)
            .count();
        long open = allComplaints.stream()
            .filter(c -> c.getStatus() == ComplaintStatus.PENDING || 
                        c.getStatus() == ComplaintStatus.IN_PROGRESS)
            .count();
        long rejected = allComplaints.stream()
            .filter(c -> c.getStatus() == ComplaintStatus.REJECTED)
            .count();
        long closed = allComplaints.stream()
            .filter(c -> c.getStatus() == ComplaintStatus.CLOSED)
            .count();
        
        // Get top agencies
        List<AgencyComplaintCount> topAgencies = getTopAgenciesByComplaints(
            allComplaints, DEFAULT_TOP_AGENCIES_LIMIT);
        
        // Get top complaint types
        List<TypeComplaintCount> topTypes = getTopComplaintTypes(
            allComplaints, DEFAULT_TOP_TYPES_LIMIT);
        
        // Calculate average resolution time
        Double avgResolutionDays = getAverageResolutionTimeDays(allComplaints);
        Double avgResolutionHours = avgResolutionDays != null ? avgResolutionDays * 24 : null;
        
        // Get overdue complaints
        List<Complaint> overdueComplaints = getOverdueComplaints(
            allComplaints, overdueDaysThreshold, DEFAULT_OVERDUE_COMPLAINTS_LIMIT);
        List<ComplaintDTOResponse> overdueDTOs = overdueComplaints.stream()
            .map(complaintMapper::toResponse)
            .collect(Collectors.toList());
        
        return DashboardOverviewDTO.builder()
            .totalComplaints(total)
            .resolvedComplaints(resolved)
            .openComplaints(open)
            .rejectedComplaints(rejected)
            .closedComplaints(closed)
            .topAgenciesByComplaints(topAgencies)
            .topComplaintTypes(topTypes)
            .averageResolutionTimeDays(avgResolutionDays)
            .averageResolutionTimeHours(avgResolutionHours)
            .overdueComplaints((long) overdueComplaints.size())
            .overdueComplaintsList(overdueDTOs)
            .fromDate(fromDate)
            .toDate(toDate)
            .overdueDaysThreshold(overdueDaysThreshold)
            .build();
    }
    
    /**
     * Get top agencies by complaint count
     */
    private List<AgencyComplaintCount> getTopAgenciesByComplaints(
            List<Complaint> complaints, int limit) {
        
        return complaints.stream()
            .collect(Collectors.groupingBy(
                Complaint::getGovernmentAgency,
                Collectors.counting()
            ))
            .entrySet().stream()
            .map(entry -> AgencyComplaintCount.builder()
                .agencyName(entry.getKey().name())
                .agencyLabel(entry.getKey().getLabel())
                .complaintCount(entry.getValue())
                .build())
            .sorted(Comparator.comparing(AgencyComplaintCount::getComplaintCount).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Get top complaint types by count
     */
    private List<TypeComplaintCount> getTopComplaintTypes(
            List<Complaint> complaints, int limit) {
        
        return complaints.stream()
            .collect(Collectors.groupingBy(
                Complaint::getComplaintType,
                Collectors.counting()
            ))
            .entrySet().stream()
            .map(entry -> TypeComplaintCount.builder()
                .typeName(entry.getKey().name())
                .typeLabel(entry.getKey().getLabel())
                .complaintCount(entry.getValue())
                .build())
            .sorted(Comparator.comparing(TypeComplaintCount::getComplaintCount).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Calculate average resolution time in days
     * Uses ComplaintHistory to accurately track when status changed to RESOLVED
     */
    private Double getAverageResolutionTimeDays(List<Complaint> complaints) {
        List<Complaint> resolvedComplaints = complaints.stream()
            .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED)
            .collect(Collectors.toList());
        
        if (resolvedComplaints.isEmpty()) {
            return 0.0;
        }
        
        List<Long> resolutionDays = new ArrayList<>();
        
        for (Complaint complaint : resolvedComplaints) {
            LocalDateTime createdTime = complaint.getCreatedAt();
            if (createdTime == null) continue;
            
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
            
            // If not found in history, use respondedAt or updatedAt
            if (resolvedTime == null) {
                if (complaint.getRespondedAt() != null) {
                    resolvedTime = complaint.getRespondedAt();
                } else if (complaint.getUpdatedAt() != null) {
                    resolvedTime = complaint.getUpdatedAt();
                } else {
                    continue; // Skip if no resolution time found
                }
            }
            
            long days = ChronoUnit.DAYS.between(createdTime, resolvedTime);
            resolutionDays.add(days);
        }
        
        if (resolutionDays.isEmpty()) {
            return 0.0;
        }
        
        return resolutionDays.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
    }
    
    /**
     * Get overdue complaints
     * Uses ComplaintHistory to determine last activity
     * A complaint is overdue if it hasn't been resolved and exceeds the threshold
     */
    private List<Complaint> getOverdueComplaints(
            List<Complaint> complaints, int daysThreshold, int limit) {
        
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(daysThreshold);
        
        return complaints.stream()
            .filter(c -> {
                // Only consider non-resolved complaints
                if (c.getStatus() == ComplaintStatus.RESOLVED || 
                    c.getStatus() == ComplaintStatus.CLOSED ||
                    c.getStatus() == ComplaintStatus.REJECTED) {
                    return false;
                }
                
                // Get last activity from ComplaintHistory
                List<ComplaintHistory> histories = complaintHistoryRepository
                    .findByComplaintIdOrderByCreatedAtDesc(c.getId());
                
                LocalDateTime lastActivity = c.getCreatedAt();
                if (!histories.isEmpty()) {
                    lastActivity = histories.get(0).getCreatedAt();
                }
                
                // Check if last activity is before threshold
                return lastActivity != null && lastActivity.isBefore(thresholdDate);
            })
            .sorted(Comparator.comparing((Complaint c) -> {
                List<ComplaintHistory> histories = complaintHistoryRepository
                    .findByComplaintIdOrderByCreatedAtDesc(c.getId());
                if (!histories.isEmpty()) {
                    return histories.get(0).getCreatedAt();
                }
                return c.getCreatedAt();
            }))
            .limit(limit)
            .collect(Collectors.toList());
    }
}

