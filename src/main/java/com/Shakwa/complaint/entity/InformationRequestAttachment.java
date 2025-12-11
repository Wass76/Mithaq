package com.Shakwa.complaint.entity;

import com.Shakwa.utils.entity.AuditedEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Junction entity linking complaint attachments to information requests
 * When a citizen provides additional information, they can upload attachments
 * that are linked to the specific information request
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(
    name = "information_request_attachments",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_info_req_att", 
        columnNames = {"information_request_id", "attachment_id"}
    )
)
@NoArgsConstructor
@AllArgsConstructor
@SequenceGenerator(name = "info_req_att_seq", sequenceName = "information_request_attachment_id_seq", allocationSize = 1)
public class InformationRequestAttachment extends AuditedEntity {
    
    @Override
    protected String getSequenceName() {
        return "information_request_attachment_id_seq";
    }
    
    /**
     * The information request this attachment belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "information_request_id", nullable = false)
    private InformationRequest informationRequest;
    
    /**
     * The complaint attachment that was uploaded in response
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id", nullable = false)
    private ComplaintAttachment attachment;
}

