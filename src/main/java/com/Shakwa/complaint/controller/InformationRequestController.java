package com.Shakwa.complaint.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.Shakwa.complaint.dto.InformationRequestCreateDTO;
import com.Shakwa.complaint.dto.InformationRequestDTO;
import com.Shakwa.complaint.dto.InformationRequestResponseDTO;
import com.Shakwa.complaint.service.InformationRequestService;
import com.Shakwa.user.dto.PaginationDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("api/v1")
@Tag(name = "Information Request Management", description = "APIs for managing information requests between employees and citizens")
public class InformationRequestController {

    private final InformationRequestService informationRequestService;

    public InformationRequestController(InformationRequestService informationRequestService) {
        this.informationRequestService = informationRequestService;
    }

    @PostMapping("complaints/{complaintId}/info-requests")
    @Operation(
        summary = "Request additional information", 
        description = "Employee requests additional information from citizen about a complaint. Only employees from the same agency can request info."
    )
    public ResponseEntity<InformationRequestDTO> requestAdditionalInfo(
            @Parameter(description = "Complaint ID", example = "123", required = true)
            @PathVariable Long complaintId,
            @Parameter(description = "Request details", required = true)
            @RequestBody InformationRequestCreateDTO dto) {
        InformationRequestDTO request = informationRequestService.requestAdditionalInfo(complaintId, dto);
        return ResponseEntity.status(201).body(request);
    }

    @PutMapping(value = "info-requests/{requestId}/respond", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Provide additional information", 
        description = "Citizen provides additional information in response to an information request. Can include text message and/or files."
    )
    public ResponseEntity<InformationRequestDTO> provideAdditionalInfo(
            @Parameter(description = "Information Request ID", example = "1", required = true)
            @PathVariable Long requestId,
            @Parameter(description = "Response message", required = false)
            @RequestPart(value = "data", required = false) InformationRequestResponseDTO dto,
            @Parameter(description = "Attachment files", required = false)
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        
        // If no DTO provided, create empty one
        if (dto == null) {
            dto = new InformationRequestResponseDTO();
        }
        
        // Validate that either message or files are provided
        if ((dto.getResponseMessage() == null || dto.getResponseMessage().trim().isEmpty()) 
            && (files == null || files.isEmpty())) {
            throw new IllegalArgumentException("Either response message or files must be provided");
        }
        
        InformationRequestDTO request = informationRequestService.provideAdditionalInfo(requestId, dto, files);
        return ResponseEntity.ok(request);
    }

    @DeleteMapping("info-requests/{requestId}")
    @Operation(
        summary = "Cancel information request", 
        description = "Employee cancels an information request. Only the requester or admin can cancel."
    )
    public ResponseEntity<Void> cancelRequest(
            @Parameter(description = "Information Request ID", example = "1", required = true)
            @PathVariable Long requestId) {
        informationRequestService.cancelRequest(requestId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("complaints/{complaintId}/info-requests")
    @Operation(
        summary = "Get information requests for a complaint", 
        description = "Get all information requests for a specific complaint. Employees see requests for their agency's complaints, citizens see their own."
    )
    public ResponseEntity<PaginationDTO<InformationRequestDTO>> getRequestsByComplaint(
            @Parameter(description = "Complaint ID", example = "123", required = true)
            @PathVariable Long complaintId,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        PaginationDTO<InformationRequestDTO> requests = informationRequestService.getRequestsByComplaint(complaintId, page, size);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("info-requests/{requestId}")
    @Operation(
        summary = "Get information request by ID", 
        description = "Get a specific information request by ID. Access is restricted based on user role and complaint ownership."
    )
    public ResponseEntity<InformationRequestDTO> getRequestById(
            @Parameter(description = "Information Request ID", example = "1", required = true)
            @PathVariable Long requestId) {
        InformationRequestDTO request = informationRequestService.getRequestById(requestId);
        return ResponseEntity.ok(request);
    }

    @GetMapping("complaints/{complaintId}/info-requests/pending")
    @Operation(
        summary = "Get pending information requests", 
        description = "Get all pending (not yet responded) information requests for a specific complaint."
    )
    public ResponseEntity<List<InformationRequestDTO>> getPendingRequests(
            @Parameter(description = "Complaint ID", example = "123", required = true)
            @PathVariable Long complaintId) {
        List<InformationRequestDTO> requests = informationRequestService.getPendingRequests(complaintId);
        return ResponseEntity.ok(requests);
    }
}

