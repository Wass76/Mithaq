package com.Shakwa.complaint.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.Shakwa.complaint.Enum.InformationRequestStatus;
import com.Shakwa.complaint.entity.InformationRequest;

@Repository
public interface InformationRequestRepository extends JpaRepository<InformationRequest, Long> {

    /**
     * Find all information requests for a specific complaint
     */
    @Query("SELECT ir FROM InformationRequest ir WHERE ir.complaint.id = :complaintId ORDER BY ir.requestedAt DESC")
    List<InformationRequest> findByComplaintId(@Param("complaintId") Long complaintId);

    /**
     * Find all information requests for a specific complaint with pagination
     */
    @Query("SELECT ir FROM InformationRequest ir WHERE ir.complaint.id = :complaintId ORDER BY ir.requestedAt DESC")
    Page<InformationRequest> findByComplaintId(@Param("complaintId") Long complaintId, Pageable pageable);

    /**
     * Find pending information requests for a specific complaint
     */
    @Query("SELECT ir FROM InformationRequest ir WHERE ir.complaint.id = :complaintId AND ir.status = :status ORDER BY ir.requestedAt DESC")
    List<InformationRequest> findByComplaintIdAndStatus(@Param("complaintId") Long complaintId, @Param("status") InformationRequestStatus status);

    /**
     * Count pending requests for a complaint
     */
    @Query("SELECT COUNT(ir) FROM InformationRequest ir WHERE ir.complaint.id = :complaintId AND ir.status = 'PENDING'")
    long countPendingByComplaintId(@Param("complaintId") Long complaintId);

    /**
     * Find information request by ID with complaint loaded
     */
    @Query("SELECT ir FROM InformationRequest ir LEFT JOIN FETCH ir.complaint WHERE ir.id = :id")
    Optional<InformationRequest> findByIdWithComplaint(@Param("id") Long id);

    /**
     * Find information request by ID with all relations loaded
     */
    @Query("SELECT ir FROM InformationRequest ir LEFT JOIN FETCH ir.complaint LEFT JOIN FETCH ir.requestedBy LEFT JOIN FETCH ir.attachments WHERE ir.id = :id")
    Optional<InformationRequest> findByIdWithRelations(@Param("id") Long id);
}

