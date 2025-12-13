package com.Shakwa.report.controller;

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

import com.Shakwa.report.dto.AverageResolutionTimeReportDTO;
import com.Shakwa.report.dto.ComplaintStatusReportDTO;
import com.Shakwa.report.dto.ComplaintTypeDistributionDTO;
import com.Shakwa.report.service.ExportService;
import com.Shakwa.report.service.ReportService;
import com.Shakwa.user.Enum.GovernmentAgencyType;

import com.itextpdf.text.DocumentException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller for report endpoints
 * Accessible to employees (their agency only) and admins (all agencies)
 */
@RestController
@RequestMapping("api/v1/reports")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPERVISOR', 'VIEWER')")
@Tag(name = "Reports", description = "APIs for generating employee reports")
public class ReportController {
    
    private final ReportService reportService;
    private final ExportService exportService;
    
    public ReportController(ReportService reportService, ExportService exportService) {
        this.reportService = reportService;
        this.exportService = exportService;
    }
    
    @GetMapping("/complaint-status")
    @Operation(
        summary = "Get complaint status report", 
        description = "Retrieve a comprehensive report showing the count of complaints grouped by status. " +
                     "The report includes counts for: RESOLVED, IN_PROGRESS, PENDING, REJECTED, and CLOSED statuses.\n\n" +
                     "**Access Control:**\n" +
                     "- **Employees (SUPERVISOR, VIEWER)**: Can only view data for their own government agency. " +
                     "The agency parameter is ignored and their agency is used automatically.\n" +
                     "- **Platform Admins (PLATFORM_ADMIN)**: Can view data for all agencies or filter by a specific agency.\n\n" +
                     "**Date Filtering:**\n" +
                     "- If `fromDate` is provided, only complaints created on or after this date are included.\n" +
                     "- If `toDate` is provided, only complaints created on or before this date are included.\n" +
                     "- If both dates are provided, complaints within the date range are included.\n" +
                     "- If no dates are provided, all complaints (within access scope) are included.\n\n" +
                     "**Agency Parameter Format:**\n" +
                     "The agency parameter accepts both formats:\n" +
                     "- Enum format with underscores: `وزارة_الصحة`\n" +
                     "- Display format with spaces: `وزارة الصحة`\n\n" +
                     "**Response:**\n" +
                     "Returns a DTO containing:\n" +
                     "- Total count of complaints\n" +
                     "- Count per status (resolved, in-progress, pending, rejected, closed)\n" +
                     "- Agency information (if filtered)\n" +
                     "- Date range information"
    )
    public ResponseEntity<ComplaintStatusReportDTO> getComplaintStatusReport(
            @Parameter(
                description = "Government agency filter (optional, admins only). " +
                             "Accepts both formats: 'وزارة_الصحة' (with underscores) or 'وزارة الصحة' (with spaces). " +
                             "For employees, this parameter is ignored and their agency is used automatically. " +
                             "Valid values include: وزارة_الصحة, وزارة_الطاقة, وزارة_التربية, etc.",
                example = "وزارة الصحة"
            )
            @RequestParam(required = false) GovernmentAgencyType agency,
            @Parameter(
                description = "Start date for filtering complaints (optional). " +
                             "Format: YYYY-MM-DD (ISO 8601). " +
                             "Only complaints created on or after this date will be included.",
                example = "2025-01-01"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(
                description = "End date for filtering complaints (optional). " +
                             "Format: YYYY-MM-DD (ISO 8601). " +
                             "Only complaints created on or before this date will be included.",
                example = "2025-01-31"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        ComplaintStatusReportDTO report = reportService.getComplaintStatusReport(agency, fromDate, toDate);
        return ResponseEntity.ok(report);
    }
    
    @GetMapping("/average-resolution-time")
    @Operation(
        summary = "Get average resolution time report", 
        description = "Retrieve a report showing the average time taken to resolve complaints. " +
                     "This report uses ComplaintHistory to accurately track when complaints changed status to RESOLVED, " +
                     "providing precise resolution time calculations.\n\n" +
                     "**Access Control:**\n" +
                     "- **Employees (SUPERVISOR, VIEWER)**: Can only view data for their own government agency. " +
                     "The agency parameter is ignored and their agency is used automatically.\n" +
                     "- **Platform Admins (PLATFORM_ADMIN)**: Can view data for all agencies or filter by a specific agency.\n\n" +
                     "**Date Filtering:**\n" +
                     "- If `fromDate` is provided, only complaints created on or after this date are included.\n" +
                     "- If `toDate` is provided, only complaints created on or before this date are included.\n" +
                     "- If both dates are provided, complaints within the date range are included.\n" +
                     "- If no dates are provided, all resolved complaints (within access scope) are included.\n\n" +
                     "**Agency Parameter Format:**\n" +
                     "The agency parameter accepts both formats:\n" +
                     "- Enum format with underscores: `وزارة_الصحة`\n" +
                     "- Display format with spaces: `وزارة الصحة`\n\n" +
                     "**Response:**\n" +
                     "Returns a DTO containing:\n" +
                     "- Average resolution time (in days, hours, minutes)\n" +
                     "- Total number of resolved complaints used in calculation\n" +
                     "- Agency information (if filtered)\n" +
                     "- Date range information"
    )
    public ResponseEntity<AverageResolutionTimeReportDTO> getAverageResolutionTimeReport(
            @Parameter(
                description = "Government agency filter (optional, admins only). " +
                             "Accepts both formats: 'وزارة_الصحة' (with underscores) or 'وزارة الصحة' (with spaces). " +
                             "For employees, this parameter is ignored and their agency is used automatically. " +
                             "Valid values include: وزارة_الصحة, وزارة_الطاقة, وزارة_التربية, etc.",
                example = "وزارة الصحة"
            )
            @RequestParam(required = false) GovernmentAgencyType agency,
            @Parameter(
                description = "Start date for filtering complaints (optional). " +
                             "Format: YYYY-MM-DD (ISO 8601). " +
                             "Only complaints created on or after this date will be included.",
                example = "2025-01-01"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(
                description = "End date for filtering complaints (optional). " +
                             "Format: YYYY-MM-DD (ISO 8601). " +
                             "Only complaints created on or before this date will be included.",
                example = "2025-01-31"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        AverageResolutionTimeReportDTO report = reportService.getAverageResolutionTimeReport(agency, fromDate, toDate);
        return ResponseEntity.ok(report);
    }
    
    @GetMapping("/complaint-type-distribution")
    @Operation(
        summary = "Get complaint type distribution report", 
        description = "Retrieve a comprehensive report showing the distribution of complaints by type. " +
                     "The report includes both counts and percentages for each complaint type, " +
                     "helping identify which types of complaints are most common.\n\n" +
                     "**Access Control:**\n" +
                     "- **Employees (SUPERVISOR, VIEWER)**: Can only view data for their own government agency. " +
                     "The agency parameter is ignored and their agency is used automatically.\n" +
                     "- **Platform Admins (PLATFORM_ADMIN)**: Can view data for all agencies or filter by a specific agency.\n\n" +
                     "**Date Filtering:**\n" +
                     "- If `fromDate` is provided, only complaints created on or after this date are included.\n" +
                     "- If `toDate` is provided, only complaints created on or before this date are included.\n" +
                     "- If both dates are provided, complaints within the date range are included.\n" +
                     "- If no dates are provided, all complaints (within access scope) are included.\n\n" +
                     "**Agency Parameter Format:**\n" +
                     "The agency parameter accepts both formats:\n" +
                     "- Enum format with underscores: `وزارة_الطاقة`\n" +
                     "- Display format with spaces: `وزارة الطاقة`\n\n" +
                     "**Response:**\n" +
                     "Returns a DTO containing:\n" +
                     "- List of complaint types with their counts\n" +
                     "- Percentage distribution for each type\n" +
                     "- Total number of complaints\n" +
                     "- Agency information (if filtered)\n" +
                     "- Date range information"
    )
    public ResponseEntity<ComplaintTypeDistributionDTO> getComplaintTypeDistribution(
            @Parameter(
                description = "Government agency filter (optional, admins only). " +
                             "Accepts both formats: 'وزارة_الطاقة' (with underscores) or 'وزارة الطاقة' (with spaces). " +
                             "For employees, this parameter is ignored and their agency is used automatically. " +
                             "Valid values include: وزارة_الصحة, وزارة_الطاقة, وزارة_التربية, etc.",
                example = "وزارة الطاقة"
            )
            @RequestParam(required = false) GovernmentAgencyType agency,
            @Parameter(
                description = "Start date for filtering complaints (optional). " +
                             "Format: YYYY-MM-DD (ISO 8601). " +
                             "Only complaints created on or after this date will be included.",
                example = "2025-01-01"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(
                description = "End date for filtering complaints (optional). " +
                             "Format: YYYY-MM-DD (ISO 8601). " +
                             "Only complaints created on or before this date will be included.",
                example = "2025-01-31"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        ComplaintTypeDistributionDTO report = reportService.getComplaintTypeDistribution(agency, fromDate, toDate);
        return ResponseEntity.ok(report);
    }
    
    @GetMapping("/complaint-status/export")
    @Operation(
        summary = "Export complaint status report", 
        description = "Export the complaint status report to a downloadable file in CSV or PDF format. " +
                     "The exported file contains the same data as the regular report endpoint but in a format " +
                     "suitable for offline analysis, sharing, or archiving.\n\n" +
                     "**Supported Formats:**\n" +
                     "- **CSV**: Comma-separated values format, suitable for Excel or spreadsheet applications\n" +
                     "- **PDF**: Portable Document Format, suitable for printing or document sharing\n\n" +
                     "**Access Control:**\n" +
                     "- **Employees (SUPERVISOR, VIEWER)**: Can only export data for their own government agency.\n" +
                     "- **Platform Admins (PLATFORM_ADMIN)**: Can export data for all agencies or filter by a specific agency.\n\n" +
                     "**Response:**\n" +
                     "Returns a file download with appropriate Content-Type and Content-Disposition headers. " +
                     "The filename will be: `complaint-status-report.{format}`"
    )
    public ResponseEntity<Resource> exportComplaintStatusReport(
            @Parameter(
                description = "Export format. Valid values: 'csv' or 'pdf'. Default: 'csv'",
                example = "csv"
            )
            @RequestParam(defaultValue = "csv") String format,
            @Parameter(
                description = "Government agency filter (optional, admins only). " +
                             "Accepts both formats: 'وزارة_الصحة' (with underscores) or 'وزارة الصحة' (with spaces).",
                example = "وزارة الصحة"
            )
            @RequestParam(required = false) GovernmentAgencyType agency,
            @Parameter(
                description = "Start date for filtering complaints (optional). Format: YYYY-MM-DD",
                example = "2025-01-01"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(
                description = "End date for filtering complaints (optional). Format: YYYY-MM-DD",
                example = "2025-01-31"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) throws IOException, DocumentException {
        
        ComplaintStatusReportDTO report = reportService.getComplaintStatusReport(agency, fromDate, toDate);
        byte[] data;
        String contentType;
        String filename;
        
        if ("pdf".equalsIgnoreCase(format)) {
            data = exportService.exportComplaintStatusReportToPDF(report);
            contentType = MediaType.APPLICATION_PDF_VALUE;
            filename = "complaint-status-report.pdf";
        } else {
            data = exportService.exportComplaintStatusReportToCSV(report);
            contentType = "text/csv";
            filename = "complaint-status-report.csv";
        }
        
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
    
    @GetMapping("/average-resolution-time/export")
    @Operation(
        summary = "Export average resolution time report", 
        description = "Export the average resolution time report to a downloadable file in CSV or PDF format. " +
                     "The exported file contains detailed resolution time statistics suitable for performance analysis.\n\n" +
                     "**Supported Formats:**\n" +
                     "- **CSV**: Comma-separated values format, suitable for Excel or spreadsheet applications\n" +
                     "- **PDF**: Portable Document Format, suitable for printing or document sharing\n\n" +
                     "**Access Control:**\n" +
                     "- **Employees (SUPERVISOR, VIEWER)**: Can only export data for their own government agency.\n" +
                     "- **Platform Admins (PLATFORM_ADMIN)**: Can export data for all agencies or filter by a specific agency.\n\n" +
                     "**Response:**\n" +
                     "Returns a file download with appropriate Content-Type and Content-Disposition headers. " +
                     "The filename will be: `average-resolution-time-report.{format}`"
    )
    public ResponseEntity<Resource> exportAverageResolutionTimeReport(
            @Parameter(
                description = "Export format. Valid values: 'csv' or 'pdf'. Default: 'csv'",
                example = "csv"
            )
            @RequestParam(defaultValue = "csv") String format,
            @Parameter(
                description = "Government agency filter (optional, admins only). " +
                             "Accepts both formats: 'وزارة_الصحة' (with underscores) or 'وزارة الصحة' (with spaces).",
                example = "وزارة الصحة"
            )
            @RequestParam(required = false) GovernmentAgencyType agency,
            @Parameter(
                description = "Start date for filtering complaints (optional). Format: YYYY-MM-DD",
                example = "2025-01-01"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(
                description = "End date for filtering complaints (optional). Format: YYYY-MM-DD",
                example = "2025-01-31"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) throws IOException, DocumentException {
        
        AverageResolutionTimeReportDTO report = reportService.getAverageResolutionTimeReport(agency, fromDate, toDate);
        byte[] data;
        String contentType;
        String filename;
        
        if ("pdf".equalsIgnoreCase(format)) {
            data = exportService.exportAverageResolutionTimeToPDF(report);
            contentType = MediaType.APPLICATION_PDF_VALUE;
            filename = "average-resolution-time-report.pdf";
        } else {
            data = exportService.exportAverageResolutionTimeToCSV(report);
            contentType = "text/csv";
            filename = "average-resolution-time-report.csv";
        }
        
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
    
    @GetMapping("/complaint-type-distribution/export")
    @Operation(
        summary = "Export complaint type distribution report", 
        description = "Export the complaint type distribution report to a downloadable file in CSV or PDF format. " +
                     "The exported file contains the distribution of complaints by type with counts and percentages.\n\n" +
                     "**Supported Formats:**\n" +
                     "- **CSV**: Comma-separated values format, suitable for Excel or spreadsheet applications\n" +
                     "- **PDF**: Portable Document Format, suitable for printing or document sharing\n\n" +
                     "**Access Control:**\n" +
                     "- **Employees (SUPERVISOR, VIEWER)**: Can only export data for their own government agency.\n" +
                     "- **Platform Admins (PLATFORM_ADMIN)**: Can export data for all agencies or filter by a specific agency.\n\n" +
                     "**Response:**\n" +
                     "Returns a file download with appropriate Content-Type and Content-Disposition headers. " +
                     "The filename will be: `complaint-type-distribution-report.{format}`"
    )
    public ResponseEntity<Resource> exportComplaintTypeDistributionReport(
            @Parameter(
                description = "Export format. Valid values: 'csv' or 'pdf'. Default: 'csv'",
                example = "csv"
            )
            @RequestParam(defaultValue = "csv") String format,
            @Parameter(
                description = "Government agency filter (optional, admins only). " +
                             "Accepts both formats: 'وزارة_الطاقة' (with underscores) or 'وزارة الطاقة' (with spaces).",
                example = "وزارة الطاقة"
            )
            @RequestParam(required = false) GovernmentAgencyType agency,
            @Parameter(
                description = "Start date for filtering complaints (optional). Format: YYYY-MM-DD",
                example = "2025-01-01"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(
                description = "End date for filtering complaints (optional). Format: YYYY-MM-DD",
                example = "2025-01-31"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) throws IOException, DocumentException {
        
        ComplaintTypeDistributionDTO report = reportService.getComplaintTypeDistribution(agency, fromDate, toDate);
        byte[] data;
        String contentType;
        String filename;
        
        if ("pdf".equalsIgnoreCase(format)) {
            data = exportService.exportComplaintTypeDistributionToPDF(report);
            contentType = MediaType.APPLICATION_PDF_VALUE;
            filename = "complaint-type-distribution-report.pdf";
        } else {
            data = exportService.exportComplaintTypeDistributionToCSV(report);
            contentType = "text/csv";
            filename = "complaint-type-distribution-report.csv";
        }
        
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}

