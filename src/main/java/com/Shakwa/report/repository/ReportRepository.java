package com.Shakwa.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Shakwa.complaint.entity.Complaint;

/**
 * Repository for report queries.
 * 
 * Note: Complex queries with optional parameters are implemented in 
 * ReportRepositoryCustomImpl using Criteria API to avoid PostgreSQL 
 * parameter type inference issues with the (:param IS NULL OR ...) pattern.
 */
@Repository
public interface ReportRepository extends JpaRepository<Complaint, Long>, ReportRepositoryCustom {
    
    // All dynamic query methods are now in ReportRepositoryCustom/ReportRepositoryCustomImpl
    // This avoids PostgreSQL "could not determine data type of parameter" errors
}
