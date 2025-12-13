package com.Shakwa.audit.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.Shakwa.audit.dto.AuditEventDTO;
import com.Shakwa.audit.service.AuditService;
import com.Shakwa.report.service.ExportService;
import com.Shakwa.user.dto.PaginationDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller for audit log management
 * Only accessible to platform admins
 */
@RestController
@RequestMapping("api/v1/admin/audit-log")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@Tag(name = "Audit Log", description = "APIs for viewing system audit logs (Admin only)")
public class AuditLogController {
    
    private final AuditService auditService;
    private final ExportService exportService;
    
    public AuditLogController(AuditService auditService, ExportService exportService) {
        this.auditService = auditService;
        this.exportService = exportService;
    }
    
    @GetMapping
    @Operation(
        summary = "Get audit logs", 
        description = "Retrieve audit logs with filters. Only platform admins can access this endpoint.\n\n" +
                     "**Available Actions:**\n" +
                     "- CREATE_COMPLAINT: Creating a new complaint\n" +
                     "- UPDATE_COMPLAINT: Updating an existing complaint\n" +
                     "- RESPOND_TO_COMPLAINT: Responding to a complaint\n" +
                     "- REQUEST_ADDITIONAL_INFO: Requesting additional information from citizen\n" +
                     "- PROVIDE_ADDITIONAL_INFO: Citizen providing additional information\n" +
                     "- CANCEL_INFO_REQUEST: Canceling an information request\n" +
                     "- REGISTER_NOTIFICATION_TOKEN: Registering a notification token\n" +
                     "- UNREGISTER_NOTIFICATION_TOKEN: Unregistering a notification token\n" +
                     "- SEND_NOTIFICATION: Sending a notification\n" +
                     "- LOGIN: User login (admin)\n" +
                     "- LOGIN_CITIZEN: Citizen login\n" +
                     "- LOGIN_EMPLOYEE: Employee login\n" +
                     "- REGISTER_CITIZEN: Citizen registration\n" +
                     "- CREATE_CITIZEN: Creating a citizen\n" +
                     "- UPDATE_CITIZEN: Updating a citizen\n" +
                     "- DELETE_CITIZEN: Deleting a citizen\n" +
                     "- CREATE_EMPLOYEE: Creating an employee\n" +
                     "- UPDATE_EMPLOYEE: Updating an employee\n" +
                     "- DELETE_EMPLOYEE: Deleting an employee\n" +
                     "- UPDATE_USER: Updating a user\n" +
                     "- DELETE_USER: Deleting a user\n" +
                     "- UPDATE_USER_PERMISSIONS: Updating user permissions\n" +
                     "- CREATE_ROLE: Creating a role\n" +
                     "- UPDATE_ROLE: Updating a role\n" +
                     "- DELETE_ROLE: Deleting a role\n" +
                     "- UPDATE_ROLE_PERMISSIONS: Updating role permissions\n" +
                     "- CHANGE_PASSWORD: Changing password\n" +
                     "- RESET_PASSWORD: Resetting password (admin)\n\n" +
                     "**Available Target Types:**\n" +
                     "- COMPLAINT: Complaint entity\n" +
                     "- INFORMATION_REQUEST: Information request entity\n" +
                     "- NOTIFICATION_TOKEN: Notification token entity\n" +
                     "- NOTIFICATION: Notification entity\n" +
                     "- USER: User entity\n" +
                     "- CITIZEN: Citizen entity\n" +
                     "- EMPLOYEE: Employee entity\n" +
                     "- ROLE: Role entity\n\n" +
                     "**Available Status Values:**\n" +
                     "- SUCCESS: Operation completed successfully\n" +
                     "- FAILURE: Operation failed"
    )
    public ResponseEntity<PaginationDTO<AuditEventDTO>> getAuditLogs(
            @Parameter(description = "Filter by user ID (actor)", example = "1")
            @RequestParam(required = false) Long userId,
            @Parameter(
                description = "Filter by action. Available options: CREATE_COMPLAINT, UPDATE_COMPLAINT, " +
                             "RESPOND_TO_COMPLAINT, REQUEST_ADDITIONAL_INFO, PROVIDE_ADDITIONAL_INFO, " +
                             "CANCEL_INFO_REQUEST, REGISTER_NOTIFICATION_TOKEN, UNREGISTER_NOTIFICATION_TOKEN, " +
                             "SEND_NOTIFICATION, LOGIN, LOGIN_CITIZEN, LOGIN_EMPLOYEE, REGISTER_CITIZEN, " +
                             "CREATE_CITIZEN, UPDATE_CITIZEN, DELETE_CITIZEN, CREATE_EMPLOYEE, UPDATE_EMPLOYEE, " +
                             "DELETE_EMPLOYEE, UPDATE_USER_PERMISSIONS, CREATE_ROLE, " +
                             "UPDATE_ROLE, DELETE_ROLE, UPDATE_ROLE_PERMISSIONS, CHANGE_PASSWORD, RESET_PASSWORD",
                example = "CREATE_COMPLAINT"
            )
            @RequestParam(required = false) String action,
            @Parameter(
                description = "Filter by target type. Available options: COMPLAINT, INFORMATION_REQUEST, " +
                             "NOTIFICATION_TOKEN, NOTIFICATION, USER, CITIZEN, EMPLOYEE, ROLE",
                example = "COMPLAINT"
            )
            @RequestParam(required = false) String targetType,
            @Parameter(description = "Filter by target ID", example = "123")
            @RequestParam(required = false) Long targetId,
            @Parameter(
                description = "Filter by status. Available options: SUCCESS, FAILURE",
                example = "SUCCESS"
            )
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by start date", example = "2025-01-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @Parameter(description = "Filter by end date", example = "2025-01-31T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        
        PaginationDTO<AuditEventDTO> auditLogs = auditService.getAuditLogs(
            userId, action, targetType, targetId, status, fromDate, toDate, page, size);
        return ResponseEntity.ok(auditLogs);
    }
    
    @GetMapping("/actor/{actorId}")
    @Operation(
        summary = "Get audit logs by actor", 
        description = "Retrieve audit logs for a specific user (actor)"
    )
    public ResponseEntity<PaginationDTO<AuditEventDTO>> getAuditLogsByActor(
            @Parameter(description = "Actor (user) ID", example = "1")
            @PathVariable Long actorId,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        PaginationDTO<AuditEventDTO> auditLogs = auditService.getAuditLogsByActor(actorId, page, size);
        return ResponseEntity.ok(auditLogs);
    }
    
    @GetMapping("/action/{action}")
    @Operation(
        summary = "Get audit logs by action", 
        description = "Retrieve audit logs for a specific action.\n\n" +
                     "**Available Actions:**\n" +
                     "- CREATE_COMPLAINT, UPDATE_COMPLAINT, RESPOND_TO_COMPLAINT\n" +
                     "- REQUEST_ADDITIONAL_INFO, PROVIDE_ADDITIONAL_INFO, CANCEL_INFO_REQUEST\n" +
                     "- REGISTER_NOTIFICATION_TOKEN, UNREGISTER_NOTIFICATION_TOKEN, SEND_NOTIFICATION\n" +
                     "- LOGIN, LOGIN_CITIZEN, LOGIN_EMPLOYEE, REGISTER_CITIZEN\n" +
                     "- CREATE_CITIZEN, UPDATE_CITIZEN, DELETE_CITIZEN\n" +
                     "- CREATE_EMPLOYEE, UPDATE_EMPLOYEE, DELETE_EMPLOYEE\n" +
                     "- UPDATE_USER, DELETE_USER, UPDATE_USER_PERMISSIONS\n" +
                     "- CREATE_ROLE, UPDATE_ROLE, DELETE_ROLE, UPDATE_ROLE_PERMISSIONS\n" +
                     "- CHANGE_PASSWORD, RESET_PASSWORD"
    )
    public ResponseEntity<PaginationDTO<AuditEventDTO>> getAuditLogsByAction(
            @Parameter(
                description = "Action name. See available actions in the endpoint description above.",
                example = "CREATE_COMPLAINT"
            )
            @PathVariable String action,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        PaginationDTO<AuditEventDTO> auditLogs = auditService.getAuditLogsByAction(action, page, size);
        return ResponseEntity.ok(auditLogs);
    }
    
    @GetMapping("/target/{targetType}/{targetId}")
    @Operation(
        summary = "Get audit logs by target", 
        description = "Retrieve audit logs for a specific target entity.\n\n" +
                     "**Available Target Types:**\n" +
                     "- COMPLAINT, INFORMATION_REQUEST, NOTIFICATION_TOKEN, NOTIFICATION\n" +
                     "- USER, CITIZEN, EMPLOYEE, ROLE"
    )
    public ResponseEntity<PaginationDTO<AuditEventDTO>> getAuditLogsByTarget(
            @Parameter(
                description = "Target type. Available options: COMPLAINT, INFORMATION_REQUEST, " +
                             "NOTIFICATION_TOKEN, NOTIFICATION, USER, CITIZEN, EMPLOYEE, ROLE",
                example = "COMPLAINT"
            )
            @PathVariable String targetType,
            @Parameter(description = "Target ID", example = "123")
            @PathVariable Long targetId,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        PaginationDTO<AuditEventDTO> auditLogs = auditService.getAuditLogsByTarget(targetType, targetId, page, size);
        return ResponseEntity.ok(auditLogs);
    }
    
    @GetMapping("/export")
    @Operation(
        summary = "Export audit log", 
        description = "Export audit log entries to a downloadable CSV file. " +
                     "The exported file contains all audit events matching the specified filters, " +
                     "suitable for compliance reporting, analysis, or archiving.\n\n" +
                     "**Access Control:**\n" +
                     "- **Platform Admins (PLATFORM_ADMIN) only**: This endpoint is restricted to platform administrators.\n\n" +
                     "**Export Format:**\n" +
                     "Currently supports CSV format only. The CSV file includes columns for:\n" +
                     "- Event ID\n" +
                     "- Timestamp\n" +
                     "- Actor (user) information\n" +
                     "- Action performed\n" +
                     "- Target type and ID\n" +
                     "- Status (SUCCESS/FAILURE)\n" +
                     "- IP address (if available)\n" +
                     "- Additional details (JSON format)\n\n" +
                     "**Filtering:**\n" +
                     "All filters from the main GET endpoint are supported. " +
                     "Only audit events matching all specified filters will be included in the export.\n\n" +
                     "**Pagination:**\n" +
                     "For exports, it's recommended to use a larger `size` parameter (default: 1000) " +
                     "to retrieve more records in a single export. " +
                     "Note that very large exports may take longer to generate.\n\n" +
                     "**Response:**\n" +
                     "Returns a CSV file download with appropriate Content-Type and Content-Disposition headers. " +
                     "The filename will be: `audit-log.csv`"
    )
    public ResponseEntity<Resource> exportAuditLog(
            @Parameter(
                description = "Export format. Currently only 'csv' is supported. Default: 'csv'",
                example = "csv"
            )
            @RequestParam(defaultValue = "csv") String format,
            @Parameter(
                description = "Filter by user ID (actor). Only audit events performed by this user will be included.",
                example = "1"
            )
            @RequestParam(required = false) Long userId,
            @Parameter(
                description = "Filter by action. See available actions in the main GET endpoint description. " +
                             "Examples: CREATE_COMPLAINT, UPDATE_COMPLAINT, LOGIN, etc.",
                example = "CREATE_COMPLAINT"
            )
            @RequestParam(required = false) String action,
            @Parameter(
                description = "Filter by target type. Available options: COMPLAINT, INFORMATION_REQUEST, " +
                             "NOTIFICATION_TOKEN, NOTIFICATION, USER, CITIZEN, EMPLOYEE, ROLE",
                example = "COMPLAINT"
            )
            @RequestParam(required = false) String targetType,
            @Parameter(
                description = "Filter by target ID. Only audit events for this specific entity will be included.",
                example = "123"
            )
            @RequestParam(required = false) Long targetId,
            @Parameter(
                description = "Filter by status. Available options: SUCCESS, FAILURE",
                example = "SUCCESS"
            )
            @RequestParam(required = false) String status,
            @Parameter(
                description = "Filter by start date. Format: YYYY-MM-DDTHH:mm:ss (ISO 8601). " +
                             "Only events on or after this date/time will be included.",
                example = "2025-01-01T00:00:00"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @Parameter(
                description = "Filter by end date. Format: YYYY-MM-DDTHH:mm:ss (ISO 8601). " +
                             "Only events on or before this date/time will be included.",
                example = "2025-01-31T23:59:59"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @Parameter(
                description = "Page size for export. For exports, use a larger value to retrieve more records. " +
                             "Default: 1000. Note: Very large exports may take longer to generate.",
                example = "1000"
            )
            @RequestParam(defaultValue = "1000") int size) throws IOException {
        
        // Get all audit events (with larger page size for export)
        PaginationDTO<AuditEventDTO> auditLogs = auditService.getAuditLogs(
            userId, action, targetType, targetId, status, fromDate, toDate, 0, size);
        
        List<AuditEventDTO> events = auditLogs.getContent();
        byte[] data = exportService.exportAuditLogToCSV(events);
        
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-log.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }
}

