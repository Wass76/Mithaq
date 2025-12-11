package com.Shakwa.complaint.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import org.apache.tika.Tika;

import com.Shakwa.complaint.Enum.InformationRequestStatus;
import com.Shakwa.complaint.dto.ComplaintAttachmentDTO;
import com.Shakwa.complaint.dto.InformationRequestCreateDTO;
import com.Shakwa.complaint.dto.InformationRequestDTO;
import com.Shakwa.complaint.dto.InformationRequestResponseDTO;
import com.Shakwa.complaint.entity.Complaint;
import com.Shakwa.complaint.entity.ComplaintAttachment;
import com.Shakwa.complaint.entity.InformationRequest;
import com.Shakwa.complaint.entity.InformationRequestAttachment;
import com.Shakwa.complaint.repository.ComplaintAttachmentRepository;
import com.Shakwa.complaint.repository.ComplaintRepository;
import com.Shakwa.complaint.repository.InformationRequestRepository;
import com.Shakwa.complaint.storage.AttachmentStorageService;
import com.Shakwa.complaint.storage.AttachmentStorageService.StoredFile;
import com.Shakwa.user.dto.PaginationDTO;
import com.Shakwa.user.entity.Citizen;
import com.Shakwa.user.entity.Employee;
import com.Shakwa.user.repository.CitizenRepo;
import com.Shakwa.user.repository.EmployeeRepository;
import com.Shakwa.user.repository.UserRepository;
import com.Shakwa.user.service.BaseSecurityService;
import com.Shakwa.utils.exception.ConflictException;
import com.Shakwa.utils.annotation.Audited;
import com.Shakwa.notification.service.ComplaintNotificationIntegration;
import com.Shakwa.utils.exception.UnAuthorizedException;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class InformationRequestService extends BaseSecurityService {

    private static final Logger logger = LoggerFactory.getLogger(InformationRequestService.class);
    
    private final InformationRequestRepository informationRequestRepository;
    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryService complaintHistoryService;
    private final ComplaintNotificationIntegration complaintNotificationIntegration;
    private final AttachmentStorageService attachmentStorageService;
    private final ComplaintAttachmentRepository complaintAttachmentRepository;
    private final Tika tika = new Tika();

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpg", "image/jpeg", "application/pdf");
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final long MAX_TOTAL_BYTES = 50 * 1024 * 1024; // 50 MB
    private static final int MAX_FILES_PER_OPERATION = 10;

    public InformationRequestService(
            InformationRequestRepository informationRequestRepository,
            ComplaintRepository complaintRepository,
            ComplaintHistoryService complaintHistoryService,
            ComplaintNotificationIntegration complaintNotificationIntegration,
            AttachmentStorageService attachmentStorageService,
            ComplaintAttachmentRepository complaintAttachmentRepository,
            UserRepository userRepository,
            CitizenRepo citizenRepo,
            EmployeeRepository employeeRepository) {
        super(userRepository, citizenRepo, employeeRepository);
        this.informationRequestRepository = informationRequestRepository;
        this.complaintRepository = complaintRepository;
        this.complaintHistoryService = complaintHistoryService;
        this.complaintNotificationIntegration = complaintNotificationIntegration;
        this.attachmentStorageService = attachmentStorageService;
        this.complaintAttachmentRepository = complaintAttachmentRepository;
    }

    /**
     * Employee requests additional information from citizen
     * Only employees from the same agency as the complaint can request info
     */
    @Audited(action = "REQUEST_ADDITIONAL_INFO", targetType = "INFORMATION_REQUEST", includeArgs = false)
    public InformationRequestDTO requestAdditionalInfo(Long complaintId, InformationRequestCreateDTO dto) {
        // Validate that current user is an employee
        if (!isCurrentUserEmployee()) {
            throw new UnAuthorizedException("Only employees can request additional information");
        }

        Employee employee = (Employee) getCurrentUser();
        if (employee.getGovernmentAgency() == null) {
            throw new UnAuthorizedException("Employee is not associated with any government agency");
        }

        // Get complaint and verify access
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new EntityNotFoundException("Complaint not found with ID: " + complaintId));

        // Verify employee has access to this complaint (same agency)
        if (!complaint.getGovernmentAgency().equals(employee.getGovernmentAgency())) {
            throw new UnAuthorizedException("You don't have access to this complaint");
        }

        // Validate request message
        if (!StringUtils.hasText(dto.getMessage())) {
            throw new ConflictException("Request message is required");
        }

        // Create information request
        InformationRequest request = new InformationRequest();
        request.setComplaint(complaint);
        request.setRequestedBy(employee);
        request.setRequestMessage(dto.getMessage());
        request.setRequestedAt(LocalDateTime.now());
        request.setStatus(InformationRequestStatus.PENDING);

        request = informationRequestRepository.save(request);

        // Record in complaint history
        complaintHistoryService.recordInfoRequested(complaint, employee, dto.getMessage());

        // Send notification to citizen
        complaintNotificationIntegration.notifyInfoRequested(complaint, request);

        logger.info("Information request created: ID={}, Complaint={}, Employee={}", 
                request.getId(), complaintId, employee.getId());

        return toDTO(request);
    }

    /**
     * Citizen provides additional information in response to a request
     * Only the complaint owner (citizen) can respond
     */
    @Audited(action = "PROVIDE_ADDITIONAL_INFO", targetType = "INFORMATION_REQUEST", includeArgs = false)
    public InformationRequestDTO provideAdditionalInfo(Long requestId, InformationRequestResponseDTO dto, List<MultipartFile> files) {
        // Validate that current user is a citizen
        if (!isCurrentUserCitizen()) {
            throw new UnAuthorizedException("Only citizens can provide additional information");
        }

        Citizen citizen = getCurrentCitizen();

        // Get information request with relations
        InformationRequest request = informationRequestRepository.findByIdWithRelations(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Information request not found with ID: " + requestId));

        // Verify request is pending
        if (request.getStatus() != InformationRequestStatus.PENDING) {
            throw new ConflictException("This information request has already been responded to or cancelled");
        }

        // Verify citizen owns the complaint
        Complaint complaint = request.getComplaint();
        if (!complaint.getCitizen().getId().equals(citizen.getId())) {
            throw new UnAuthorizedException("You don't have access to this information request");
        }

        // Validate response: must have either message or files
        boolean hasMessage = StringUtils.hasText(dto.getResponseMessage());
        boolean hasFiles = files != null && !files.isEmpty();

        if (!hasMessage && !hasFiles) {
            throw new ConflictException("Either response message or files must be provided");
        }

        // Update request
        if (hasMessage) {
            request.setResponseMessage(dto.getResponseMessage());
        }
        request.setRespondedAt(LocalDateTime.now());
        request.setStatus(InformationRequestStatus.RESPONDED);

        // Handle file uploads if provided
        if (hasFiles) {
            validateFiles(files);
            List<ComplaintAttachment> newAttachments = storeAttachments(complaint, files, citizen);
            
            // Link attachments to information request
            for (ComplaintAttachment attachment : newAttachments) {
                InformationRequestAttachment link = new InformationRequestAttachment();
                link.setInformationRequest(request);
                link.setAttachment(attachment);
                if (request.getAttachments() == null) {
                    request.setAttachments(new ArrayList<>());
                }
                request.getAttachments().add(link);
            }
        }

        request = informationRequestRepository.save(request);

        // Record in complaint history
        complaintHistoryService.recordInfoProvided(complaint, citizen, request);

        // Optionally notify employee (can be added later)
        // complaintNotificationIntegration.notifyInfoProvided(complaint, request);

        logger.info("Information request responded: ID={}, Complaint={}, Citizen={}", 
                requestId, complaint.getId(), citizen.getId());

        return toDTO(request);
    }

    /**
     * Employee cancels an information request
     * Only the requester or admin can cancel
     */
    @Audited(action = "CANCEL_INFO_REQUEST", targetType = "INFORMATION_REQUEST", includeArgs = false)
    public void cancelRequest(Long requestId) {
        // Validate that current user is an employee
        if (!isCurrentUserEmployee()) {
            throw new UnAuthorizedException("Only employees can cancel information requests");
        }

        Employee employee = (Employee) getCurrentUser();

        // Get information request
        InformationRequest request = informationRequestRepository.findByIdWithComplaint(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Information request not found with ID: " + requestId));

        // Verify request is pending
        if (request.getStatus() != InformationRequestStatus.PENDING) {
            throw new ConflictException("Only pending requests can be cancelled");
        }

        // Verify employee is the requester or is admin
        if (!request.getRequestedBy().getId().equals(employee.getId()) && !isAdmin()) {
            throw new UnAuthorizedException("Only the requester or admin can cancel this request");
        }

        // Verify employee has access to the complaint
        Complaint complaint = request.getComplaint();
        if (!complaint.getGovernmentAgency().equals(employee.getGovernmentAgency()) && !isAdmin()) {
            throw new UnAuthorizedException("You don't have access to this information request");
        }

        // Update request status
        request.setStatus(InformationRequestStatus.CANCELLED);
        informationRequestRepository.save(request);

        // Record in complaint history
        complaintHistoryService.recordInfoRequestCancelled(complaint, employee, request);

        logger.info("Information request cancelled: ID={}, Complaint={}, Employee={}", 
                requestId, complaint.getId(), employee.getId());
    }

    /**
     * Get all information requests for a complaint
     * Employees see requests for their agency's complaints, citizens see their own
     */
    public PaginationDTO<InformationRequestDTO> getRequestsByComplaint(Long complaintId, int page, int size) {
        // Verify complaint exists and user has access
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new EntityNotFoundException("Complaint not found with ID: " + complaintId));

        // Check access
        if (isCurrentUserCitizen()) {
            Citizen citizen = getCurrentCitizen();
            if (!complaint.getCitizen().getId().equals(citizen.getId())) {
                throw new UnAuthorizedException("You don't have access to this complaint");
            }
        } else if (isCurrentUserEmployee()) {
            Employee employee = (Employee) getCurrentUser();
            if (employee.getGovernmentAgency() == null || 
                !complaint.getGovernmentAgency().equals(employee.getGovernmentAgency())) {
                if (!isAdmin()) {
                    throw new UnAuthorizedException("You don't have access to this complaint");
                }
            }
        }

        // Get requests with pagination
        Pageable pageable = PageRequest.of(page, size);
        Page<InformationRequest> requestPage = informationRequestRepository.findByComplaintId(complaintId, pageable);

        Page<InformationRequestDTO> dtoPage = requestPage.map(this::toDTO);
        return PaginationDTO.of(dtoPage);
    }

    /**
     * Get a single information request by ID
     */
    public InformationRequestDTO getRequestById(Long requestId) {
        InformationRequest request = informationRequestRepository.findByIdWithRelations(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Information request not found with ID: " + requestId));

        Complaint complaint = request.getComplaint();

        // Check access
        if (isCurrentUserCitizen()) {
            Citizen citizen = getCurrentCitizen();
            if (!complaint.getCitizen().getId().equals(citizen.getId())) {
                throw new UnAuthorizedException("You don't have access to this information request");
            }
        } else if (isCurrentUserEmployee()) {
            Employee employee = (Employee) getCurrentUser();
            if (employee.getGovernmentAgency() == null || 
                !complaint.getGovernmentAgency().equals(employee.getGovernmentAgency())) {
                if (!isAdmin()) {
                    throw new UnAuthorizedException("You don't have access to this information request");
                }
            }
        }

        return toDTO(request);
    }

    /**
     * Get pending requests for a complaint
     */
    public List<InformationRequestDTO> getPendingRequests(Long complaintId) {
        // Verify access (same as getRequestsByComplaint)
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new EntityNotFoundException("Complaint not found with ID: " + complaintId));

        if (isCurrentUserCitizen()) {
            Citizen citizen = getCurrentCitizen();
            if (!complaint.getCitizen().getId().equals(citizen.getId())) {
                throw new UnAuthorizedException("You don't have access to this complaint");
            }
        } else if (isCurrentUserEmployee()) {
            Employee employee = (Employee) getCurrentUser();
            if (employee.getGovernmentAgency() == null || 
                !complaint.getGovernmentAgency().equals(employee.getGovernmentAgency())) {
                if (!isAdmin()) {
                    throw new UnAuthorizedException("You don't have access to this complaint");
                }
            }
        }

        List<InformationRequest> requests = informationRequestRepository.findByComplaintIdAndStatus(
                complaintId, InformationRequestStatus.PENDING);

        return requests.stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Convert entity to DTO
     */
    private InformationRequestDTO toDTO(InformationRequest request) {
        InformationRequestDTO.RequesterInfo requesterInfo = InformationRequestDTO.RequesterInfo.builder()
                .id(request.getRequestedBy().getId())
                .name(request.getRequestedBy().getFirstName() + " " + request.getRequestedBy().getLastName())
                .email(request.getRequestedBy().getEmail())
                .build();

        List<ComplaintAttachmentDTO> attachmentDTOs = new ArrayList<>();
        if (request.getAttachments() != null) {
            for (InformationRequestAttachment link : request.getAttachments()) {
                ComplaintAttachment attachment = link.getAttachment();
                ComplaintAttachmentDTO dto = ComplaintAttachmentDTO.builder()
                        .id(attachment.getId())
                        .originalFilename(attachment.getOriginalFilename())
                        .size(attachment.getSize())
                        .contentType(attachment.getContentType())
                        .uploadedAt(attachment.getUploadedAt())
                        .downloadUrl("/api/v1/complaints/" + request.getComplaint().getId() + 
                                    "/attachments/" + attachment.getId())
                        .build();
                attachmentDTOs.add(dto);
            }
        }

        return InformationRequestDTO.builder()
                .id(request.getId())
                .complaintId(request.getComplaint().getId())
                .requestedBy(requesterInfo)
                .requestMessage(request.getRequestMessage())
                .status(request.getStatus())
                .requestedAt(request.getRequestedAt())
                .respondedAt(request.getRespondedAt())
                .responseMessage(request.getResponseMessage())
                .attachments(attachmentDTOs)
                .build();
    }

    /**
     * Store attachments and return list of created ComplaintAttachment entities
     */
    private List<ComplaintAttachment> storeAttachments(Complaint complaint, List<MultipartFile> files, Citizen citizen) {
        List<ComplaintAttachment> attachments = new ArrayList<>();
        
        for (MultipartFile file : files) {
            String detectedType = detectContentType(file);
            if (!ALLOWED_CONTENT_TYPES.contains(detectedType)) {
                throw new ConflictException("Unsupported file type: " + detectedType);
            }
            
            StoredFile stored = attachmentStorageService.store(file, complaint.getTrackingNumber());
            ComplaintAttachment attachment = new ComplaintAttachment();
            attachment.setComplaint(complaint);
            attachment.setOriginalFilename(file.getOriginalFilename());
            attachment.setStoredFilename(stored.storedFilename());
            attachment.setStoragePath(stored.relativePath());
            attachment.setContentType(detectedType);
            attachment.setSize(stored.size());
            attachment.setChecksum(stored.checksum());
            attachment.setUploadedBy(citizen);
            attachment.setUploadedAt(LocalDateTime.now());
            
            attachment = complaintAttachmentRepository.save(attachment);
            attachments.add(attachment);
        }
        
        return attachments;
    }

    /**
     * Validate files
     */
    private void validateFiles(List<MultipartFile> files) {
        if (files.size() > MAX_FILES_PER_OPERATION) {
            throw new ConflictException("Cannot upload more than " + MAX_FILES_PER_OPERATION + " files at once");
        }
        long totalSize = 0;
        for (MultipartFile file : files) {
            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new ConflictException("File " + file.getOriginalFilename() + " exceeds maximum size of 10 MB");
            }
            totalSize += file.getSize();
        }
        if (totalSize > MAX_TOTAL_BYTES) {
            throw new ConflictException("Total attachments size exceeds 50 MB limit");
        }
    }

    /**
     * Detect content type
     */
    private String detectContentType(MultipartFile file) {
        try {
            return tika.detect(file.getInputStream(), file.getOriginalFilename());
        } catch (Exception e) {
            throw new ConflictException("Unable to determine file type");
        }
    }
}

