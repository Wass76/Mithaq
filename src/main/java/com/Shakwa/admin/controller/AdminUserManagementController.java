package com.Shakwa.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.Shakwa.admin.dto.SuspendRequest;
import com.Shakwa.user.Enum.GovernmentAgencyType;
import com.Shakwa.user.service.CitizenService;
import com.Shakwa.user.service.EmployeeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controller for Admin User Management
 * Only accessible to platform admins
 */
@RestController
@RequestMapping("api/v1/admin/users")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@Tag(name = "Admin User Management", description = "APIs for managing users and employees (Admin only)")
public class AdminUserManagementController {
    
    private final EmployeeService employeeService;
    private final CitizenService citizenService;
    
    public AdminUserManagementController(EmployeeService employeeService,
                                        CitizenService citizenService) {
        this.employeeService = employeeService;
        this.citizenService = citizenService;
    }
    
    // Employee Management
    
    @PutMapping("/employees/{id}/disable")
    @Operation(
        summary = "Disable employee", 
        description = "Disable an employee account by setting their status to INACTIVE. " +
                     "A disabled employee cannot log in to the system or perform any actions.\n\n" +
                     "**Access Control:**\n" +
                     "- **Platform Admins (PLATFORM_ADMIN) only**: This endpoint is restricted to platform administrators.\n\n" +
                     "**Important Notes:**\n" +
                     "- Disabling an employee immediately revokes their access to the system.\n" +
                     "- The employee's existing data and assigned complaints are not affected.\n" +
                     "- This operation is logged in the audit log.\n" +
                     "- To re-enable the employee, use the `/employees/{id}/enable` endpoint.\n\n" +
                     "**Response:**\n" +
                     "Returns 200 OK if the employee is successfully disabled. " +
                     "Returns 404 if the employee is not found."
    )
    public ResponseEntity<Void> disableEmployee(
            @Parameter(
                description = "Employee ID. The ID of the employee to disable.",
                example = "1"
            )
            @PathVariable Long id) {
        employeeService.disableEmployee(id);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/employees/{id}/enable")
    @Operation(
        summary = "Enable employee", 
        description = "Enable an employee account by setting their status to ACTIVE. " +
                     "An enabled employee can log in to the system and perform actions according to their role.\n\n" +
                     "**Access Control:**\n" +
                     "- **Platform Admins (PLATFORM_ADMIN) only**: This endpoint is restricted to platform administrators.\n\n" +
                     "**Important Notes:**\n" +
                     "- Enabling an employee immediately restores their access to the system.\n" +
                     "- The employee will have the same permissions and agency assignment as before being disabled.\n" +
                     "- This operation is logged in the audit log.\n\n" +
                     "**Response:**\n" +
                     "Returns 200 OK if the employee is successfully enabled. " +
                     "Returns 404 if the employee is not found."
    )
    public ResponseEntity<Void> enableEmployee(
            @Parameter(
                description = "Employee ID. The ID of the employee to enable.",
                example = "1"
            )
            @PathVariable Long id) {
        employeeService.enableEmployee(id);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/employees/{id}/role")
    @Operation(
        summary = "Update employee role", 
        description = "Update an employee's role assignment. " +
                     "Changing an employee's role modifies their permissions and access levels in the system.\n\n" +
                     "**Access Control:**\n" +
                     "- **Platform Admins (PLATFORM_ADMIN) only**: This endpoint is restricted to platform administrators.\n\n" +
                     "**Important Notes:**\n" +
                     "- Changing an employee's role immediately affects their permissions.\n" +
                     "- The employee must log out and log back in for permission changes to take full effect.\n" +
                     "- This operation is logged in the audit log.\n" +
                     "- Valid role names include: PLATFORM_ADMIN, SUPERVISOR, VIEWER, etc.\n\n" +
                     "**Response:**\n" +
                     "Returns 200 OK if the role is successfully updated. " +
                     "Returns 404 if the employee is not found. " +
                     "Returns 400 if the role name is invalid."
    )
    public ResponseEntity<Void> updateEmployeeRole(
            @Parameter(
                description = "Employee ID. The ID of the employee whose role will be updated.",
                example = "1"
            )
            @PathVariable Long id,
            @Parameter(
                description = "New role name. Valid values include: PLATFORM_ADMIN, SUPERVISOR, VIEWER, etc.",
                example = "SUPERVISOR"
            )
            @RequestParam String roleName) {
        employeeService.updateEmployeeRole(id, roleName);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/employees/{id}/agency")
    @Operation(
        summary = "Update employee agency", 
        description = "Update an employee's government agency assignment. " +
                     "This operation changes which government agency the employee belongs to, " +
                     "which affects their access scope and the complaints they can view/manage.\n\n" +
                     "**Access Control:**\n" +
                     "- **Platform Admins (PLATFORM_ADMIN) only**: This endpoint is restricted to platform administrators.\n\n" +
                     "**Important Notes:**\n" +
                     "- Changing an employee's agency will immediately affect their access to complaints.\n" +
                     "- The employee will only be able to access complaints for their new agency.\n" +
                     "- This operation is logged in the audit log.\n\n" +
                     "**Agency Parameter Format:**\n" +
                     "The agency parameter accepts both formats:\n" +
                     "- Enum format with underscores: `وزارة_الصحة`\n" +
                     "- Display format with spaces: `وزارة الصحة`\n\n" +
                     "**Response:**\n" +
                     "Returns 200 OK if the update is successful. " +
                     "Returns 404 if the employee is not found. " +
                     "Returns 400 if the agency value is invalid."
    )
    public ResponseEntity<Void> updateEmployeeAgency(
            @Parameter(
                description = "Employee ID. The ID of the employee whose agency will be updated.",
                example = "1"
            )
            @PathVariable Long id,
            @Parameter(
                description = "New government agency. " +
                             "Accepts both formats: 'وزارة_الصحة' (with underscores) or 'وزارة الصحة' (with spaces). " +
                             "Valid values include: وزارة_الصحة, وزارة_الطاقة, وزارة_التربية, etc.",
                example = "وزارة الصحة"
            )
            @RequestParam GovernmentAgencyType agency) {
        employeeService.updateEmployeeAgency(id, agency);
        return ResponseEntity.ok().build();
    }
    
    // Citizen Management
    
    @PutMapping("/citizens/{id}/suspend")
    @Operation(
        summary = "Suspend citizen", 
        description = "Suspend a citizen account by setting their status to INACTIVE. " +
                     "A suspended citizen cannot log in to the system or submit new complaints.\n\n" +
                     "**Access Control:**\n" +
                     "- **Platform Admins (PLATFORM_ADMIN) only**: This endpoint is restricted to platform administrators.\n\n" +
                     "**Important Notes:**\n" +
                     "- Suspending a citizen immediately revokes their access to the system.\n" +
                     "- The citizen cannot submit new complaints while suspended.\n" +
                     "- Existing complaints submitted by the citizen are not affected.\n" +
                     "- A reason for suspension must be provided in the request body.\n" +
                     "- This operation is logged in the audit log.\n" +
                     "- To unsuspend the citizen, use the `/citizens/{id}/unsuspend` endpoint.\n\n" +
                     "**Request Body:**\n" +
                     "Must include a `reason` field explaining why the citizen is being suspended.\n\n" +
                     "**Response:**\n" +
                     "Returns 200 OK if the citizen is successfully suspended. " +
                     "Returns 404 if the citizen is not found. " +
                     "Returns 400 if the request body is invalid."
    )
    public ResponseEntity<Void> suspendCitizen(
            @Parameter(
                description = "Citizen ID. The ID of the citizen to suspend.",
                example = "1"
            )
            @PathVariable Long id,
            @Parameter(
                description = "Suspension request containing the reason for suspension. " +
                             "The reason field is required and should explain why the citizen is being suspended.",
                required = true
            )
            @Valid @RequestBody SuspendRequest request) {
        citizenService.suspendCitizen(id, request.getReason());
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/citizens/{id}/unsuspend")
    @Operation(
        summary = "Unsuspend citizen", 
        description = "Unsuspend a citizen account by setting their status to ACTIVE. " +
                     "An unsuspended citizen can log in to the system and submit complaints again.\n\n" +
                     "**Access Control:**\n" +
                     "- **Platform Admins (PLATFORM_ADMIN) only**: This endpoint is restricted to platform administrators.\n\n" +
                     "**Important Notes:**\n" +
                     "- Unsuspending a citizen immediately restores their access to the system.\n" +
                     "- The citizen can resume submitting complaints and accessing their account.\n" +
                     "- This operation is logged in the audit log.\n\n" +
                     "**Response:**\n" +
                     "Returns 200 OK if the citizen is successfully unsuspended. " +
                     "Returns 404 if the citizen is not found."
    )
    public ResponseEntity<Void> unsuspendCitizen(
            @Parameter(
                description = "Citizen ID. The ID of the citizen to unsuspend.",
                example = "1"
            )
            @PathVariable Long id) {
        citizenService.unsuspendCitizen(id);
        return ResponseEntity.ok().build();
    }
}

