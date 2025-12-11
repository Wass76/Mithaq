package com.Shakwa.complaint.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.Shakwa.complaint.Enum.InformationRequestStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Information Request
 * Used for both request creation and response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Information Request DTO")
public class InformationRequestDTO {

    @Schema(description = "Information Request ID", example = "1")
    private Long id;

    @Schema(description = "Complaint ID", example = "123")
    private Long complaintId;

    @Schema(description = "Employee who requested the information")
    private RequesterInfo requestedBy;

    @Schema(description = "Request message from employee", example = "نحتاج إلى صورة واضحة للوثيقة المرفقة")
    private String requestMessage;

    @Schema(description = "Status of the request", example = "PENDING")
    private InformationRequestStatus status;

    @Schema(description = "When the request was created")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestedAt;

    @Schema(description = "When the citizen provided the information")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime respondedAt;

    @Schema(description = "Response message from citizen", example = "إليكم الصورة المطلوبة")
    private String responseMessage;

    @Schema(description = "Attachments uploaded in response to this request")
    @Builder.Default
    private List<ComplaintAttachmentDTO> attachments = new ArrayList<>();

    /**
     * Nested DTO for requester information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Requester Information")
    public static class RequesterInfo {
        @Schema(description = "Employee ID", example = "5")
        private Long id;

        @Schema(description = "Employee name", example = "أحمد محمد")
        private String name;

        @Schema(description = "Employee email", example = "ahmed@example.com")
        private String email;
    }
}

