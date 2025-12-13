package com.Shakwa.complaint.entity;

import java.time.LocalDateTime;

import com.Shakwa.user.entity.BaseUser;
import com.Shakwa.utils.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "complaint_attachments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SequenceGenerator(name = "complaint_attachment_seq", sequenceName = "complaint_attachment_id_seq", allocationSize = 1)
public class ComplaintAttachment extends BaseEntity {

    @Override
    protected String getSequenceName() {
        return "complaint_attachment_id_seq";
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long size;

    @Column(name = "checksum", nullable = false, length = 128)
    private String checksum;

    /**
     * معرف المستخدم الذي رفع المرفق
     */
    @Column(name = "uploaded_by_id")
    private Long uploadedById;
    
    /**
     * اسم المستخدم الذي رفع المرفق (محفوظ للعرض)
     */
    @Column(name = "uploaded_by_name")
    private String uploadedByName;
    
    /**
     * نوع المستخدم (USER, CITIZEN, EMPLOYEE)
     */
    @Column(name = "uploaded_by_type")
    private String uploadedByType;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
    
    /**
     * Helper method to set uploader info from BaseUser
     */
    public void setUploadedBy(BaseUser user) {
        if (user != null) {
            this.uploadedById = user.getId();
            this.uploadedByName = user.getFirstName() + " " + user.getLastName();
            this.uploadedByType = user.getClass().getSimpleName().toUpperCase();
        }
    }
}


