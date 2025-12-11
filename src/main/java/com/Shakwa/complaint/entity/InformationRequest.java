package com.Shakwa.complaint.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.Shakwa.complaint.Enum.InformationRequestStatus;
import com.Shakwa.user.entity.Employee;
import com.Shakwa.utils.entity.AuditedEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Information Request Entity
 * Allows employees to request additional information from citizens about their complaints
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "information_requests")
@NoArgsConstructor
@AllArgsConstructor
@SequenceGenerator(name = "info_request_seq", sequenceName = "information_request_id_seq", allocationSize = 1)
public class InformationRequest extends AuditedEntity {
    
    @Override
    protected String getSequenceName() {
        return "information_request_id_seq";
    }
    
    /**
     * The complaint this request is related to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;
    
    /**
     * Employee who requested the information
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private Employee requestedBy;
    
    /**
     * When the request was created
     */
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;
    
    /**
     * Message from employee explaining what information is needed
     */
    @Column(name = "request_message", columnDefinition = "TEXT", nullable = false)
    private String requestMessage;
    
    /**
     * Status of the request: PENDING, RESPONDED, CANCELLED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InformationRequestStatus status = InformationRequestStatus.PENDING;
    
    /**
     * When the citizen provided the information
     */
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
    
    /**
     * Response message from citizen
     */
    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;
    
    /**
     * Optimistic locking version
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    
    /**
     * Attachments uploaded in response to this request
     */
    @OneToMany(mappedBy = "informationRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InformationRequestAttachment> attachments = new ArrayList<>();
}

