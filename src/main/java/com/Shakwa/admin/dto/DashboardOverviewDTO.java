package com.Shakwa.admin.dto;

import java.time.LocalDate;
import java.util.List;

import com.Shakwa.complaint.dto.ComplaintDTOResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Admin Dashboard Overview
 * Provides comprehensive statistics and KPIs for platform admins
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewDTO {
    
    // إجمالي الشكاوى
    /**
     * Total number of complaints
     */
    private Long totalComplaints;
    
    /**
     * Number of resolved complaints
     */
    private Long resolvedComplaints;
    
    /**
     * Number of open complaints (PENDING + IN_PROGRESS)
     */
    private Long openComplaints;
    
    /**
     * Number of rejected complaints
     */
    private Long rejectedComplaints;
    
    /**
     * Number of closed complaints
     */
    private Long closedComplaints;
    
    // أكثر الجهات استقبالاً
    /**
     * Top agencies by complaint count
     */
    private List<AgencyComplaintCount> topAgenciesByComplaints;
    
    // أكثر أنواع الشكاوى شيوعاً
    /**
     * Top complaint types by count
     */
    private List<TypeComplaintCount> topComplaintTypes;
    
    // متوسط زمن الإغلاق العام
    /**
     * Average resolution time in days (across all agencies)
     */
    private Double averageResolutionTimeDays;
    
    /**
     * Average resolution time in hours
     */
    private Double averageResolutionTimeHours;
    
    // الشكاوى المتأخرة
    /**
     * Number of overdue complaints
     */
    private Long overdueComplaints;
    
    /**
     * List of overdue complaints (limited to top N)
     */
    private List<ComplaintDTOResponse> overdueComplaintsList;
    
    // فترة الإحصائيات
    /**
     * Start date of the statistics period
     */
    private LocalDate fromDate;
    
    /**
     * End date of the statistics period
     */
    private LocalDate toDate;
    
    /**
     * Days threshold for considering a complaint overdue (default: 30 days)
     */
    private Integer overdueDaysThreshold;
    
    /**
     * Inner class for agency complaint count
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgencyComplaintCount {
        private String agencyName;
        private String agencyLabel;
        private Long complaintCount;
    }
    
    /**
     * Inner class for type complaint count
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeComplaintCount {
        private String typeName;
        private String typeLabel;
        private Long complaintCount;
    }
}

