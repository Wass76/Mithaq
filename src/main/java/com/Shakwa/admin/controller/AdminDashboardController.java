package com.Shakwa.admin.controller;

import java.io.IOException;
import java.time.LocalDate;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.Shakwa.admin.dto.DashboardOverviewDTO;
import com.Shakwa.admin.service.AdminDashboardService;
import com.Shakwa.report.service.ExportService;

import com.itextpdf.text.DocumentException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller for Admin Dashboard
 * Only accessible to platform admins
 */
@RestController
@RequestMapping("api/v1/admin/dashboard")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@Tag(name = "Admin Dashboard", description = "APIs for admin dashboard overview (Admin only)")
public class AdminDashboardController {
    
    private final AdminDashboardService adminDashboardService;
    private final ExportService exportService;
    
    public AdminDashboardController(AdminDashboardService adminDashboardService, ExportService exportService) {
        this.adminDashboardService = adminDashboardService;
        this.exportService = exportService;
    }
    
    @GetMapping("/overview")
    @Operation(
        summary = "Get dashboard overview", 
        description = "Retrieve comprehensive statistics and Key Performance Indicators (KPIs) for platform administrators. " +
                     "This endpoint provides a high-level overview of the entire platform's performance and activity.\n\n" +
                     "**Access Control:**\n" +
                     "- **Platform Admins (PLATFORM_ADMIN) only**: This endpoint is restricted to platform administrators.\n\n" +
                     "**Report Contents:**\n" +
                     "The dashboard overview includes:\n" +
                     "- **Total Complaints**: Count of all complaints in the system (within date range if specified)\n" +
                     "- **Top Agencies**: List of government agencies with the most complaints\n" +
                     "- **Top Complaint Types**: Most common types of complaints\n" +
                     "- **Average Resolution Time**: Average time taken to resolve complaints across all agencies\n" +
                     "- **Overdue Complaints**: Complaints that have exceeded the specified threshold days without resolution\n\n" +
                     "**Date Filtering:**\n" +
                     "- If `fromDate` is not provided, it defaults to 30 days ago from today.\n" +
                     "- If `toDate` is not provided, it defaults to today.\n" +
                     "- Both dates are inclusive.\n\n" +
                     "**Overdue Complaints:**\n" +
                     "- Overdue complaints are those that have been in a non-resolved status (PENDING, IN_PROGRESS) " +
                     "for longer than the `overdueDaysThreshold`.\n" +
                     "- If `overdueDaysThreshold` is not provided, it defaults to 30 days.\n\n" +
                     "**Response:**\n" +
                     "Returns a comprehensive DTO containing all dashboard metrics and statistics."
    )
    public ResponseEntity<DashboardOverviewDTO> getDashboardOverview(
            @Parameter(
                description = "Start date for filtering data (optional). " +
                             "Format: YYYY-MM-DD (ISO 8601). " +
                             "If not provided, defaults to 30 days ago from today. " +
                             "Only data from this date onwards will be included.",
                example = "2025-01-01"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(
                description = "End date for filtering data (optional). " +
                             "Format: YYYY-MM-DD (ISO 8601). " +
                             "If not provided, defaults to today. " +
                             "Only data up to this date will be included.",
                example = "2025-01-31"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(
                description = "Days threshold for determining overdue complaints (optional). " +
                             "Complaints that have been in a non-resolved status for longer than this number of days " +
                             "will be considered overdue. " +
                             "If not provided, defaults to 30 days.",
                example = "30"
            )
            @RequestParam(required = false) Integer overdueDaysThreshold) {
        
        DashboardOverviewDTO overview = adminDashboardService.getDashboardOverview(
            fromDate, toDate, overdueDaysThreshold);
        return ResponseEntity.ok(overview);
    }
    
    @GetMapping("/export")
    @Operation(
        summary = "Export dashboard overview", 
        description = "Export the dashboard overview to a downloadable PDF file. " +
                     "The exported file contains all the same statistics and KPIs as the regular overview endpoint " +
                     "but in a format suitable for printing, sharing, or archiving.\n\n" +
                     "**Access Control:**\n" +
                     "- **Platform Admins (PLATFORM_ADMIN) only**: This endpoint is restricted to platform administrators.\n\n" +
                     "**Export Format:**\n" +
                     "Currently supports PDF format only. The PDF includes:\n" +
                     "- All dashboard statistics and KPIs\n" +
                     "- Formatted tables for top agencies and complaint types\n" +
                     "- Charts and visualizations (if applicable)\n" +
                     "- Date range information\n" +
                     "- Generation timestamp\n\n" +
                     "**Date Filtering:**\n" +
                     "- If `fromDate` is not provided, it defaults to 30 days ago from today.\n" +
                     "- If `toDate` is not provided, it defaults to today.\n" +
                     "- Both dates are inclusive.\n\n" +
                     "**Response:**\n" +
                     "Returns a PDF file download with appropriate Content-Type and Content-Disposition headers. " +
                     "The filename will be: `dashboard-overview.pdf`"
    )
    public ResponseEntity<Resource> exportDashboardOverview(
            @Parameter(
                description = "Export format. Currently only 'pdf' is supported. Default: 'pdf'",
                example = "pdf"
            )
            @RequestParam(defaultValue = "pdf") String format,
            @Parameter(
                description = "Start date for filtering data (optional). Format: YYYY-MM-DD. Defaults to 30 days ago.",
                example = "2025-01-01"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(
                description = "End date for filtering data (optional). Format: YYYY-MM-DD. Defaults to today.",
                example = "2025-01-31"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(
                description = "Days threshold for overdue complaints (optional). Defaults to 30 days.",
                example = "30"
            )
            @RequestParam(required = false) Integer overdueDaysThreshold) throws IOException, DocumentException {
        
        DashboardOverviewDTO dashboard = adminDashboardService.getDashboardOverview(
            fromDate, toDate, overdueDaysThreshold);
        
        byte[] data = exportService.exportDashboardOverviewToPDF(dashboard);
        
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dashboard-overview.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}

