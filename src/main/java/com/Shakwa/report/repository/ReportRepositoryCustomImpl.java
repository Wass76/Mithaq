package com.Shakwa.report.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.Shakwa.complaint.Enum.ComplaintStatus;
import com.Shakwa.complaint.entity.Complaint;
import com.Shakwa.user.Enum.GovernmentAgencyType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Custom repository implementation for report queries.
 * Uses Criteria API for dynamic query building to avoid PostgreSQL 
 * parameter type inference issues that occur with the (:param IS NULL OR ...) pattern.
 */
@Repository
public class ReportRepositoryCustomImpl implements ReportRepositoryCustom {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public List<Object[]> countComplaintsByStatus(
            GovernmentAgencyType agency,
            LocalDateTime fromDate,
            LocalDateTime toDate) {
        
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Complaint> root = query.from(Complaint.class);
        
        // Build predicates dynamically
        List<Predicate> predicates = buildCommonPredicates(cb, root, agency, fromDate, toDate);
        
        query.multiselect(root.get("status"), cb.count(root))
             .where(predicates.toArray(new Predicate[0]))
             .groupBy(root.get("status"));
        
        return entityManager.createQuery(query).getResultList();
    }
    
    @Override
    public List<Object[]> countComplaintsByType(
            GovernmentAgencyType agency,
            LocalDateTime fromDate,
            LocalDateTime toDate) {
        
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Complaint> root = query.from(Complaint.class);
        
        // Build predicates dynamically
        List<Predicate> predicates = buildCommonPredicates(cb, root, agency, fromDate, toDate);
        
        query.multiselect(root.get("complaintType"), cb.count(root))
             .where(predicates.toArray(new Predicate[0]))
             .groupBy(root.get("complaintType"))
             .orderBy(cb.desc(cb.count(root)));
        
        return entityManager.createQuery(query).getResultList();
    }
    
    @Override
    public Long countTotalComplaints(
            GovernmentAgencyType agency,
            LocalDateTime fromDate,
            LocalDateTime toDate) {
        
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Complaint> root = query.from(Complaint.class);
        
        // Build predicates dynamically
        List<Predicate> predicates = buildCommonPredicates(cb, root, agency, fromDate, toDate);
        
        query.select(cb.count(root))
             .where(predicates.toArray(new Predicate[0]));
        
        Long result = entityManager.createQuery(query).getSingleResult();
        return result != null ? result : 0L;
    }
    
    @Override
    public Long countComplaintsByStatusValue(
            ComplaintStatus status,
            GovernmentAgencyType agency,
            LocalDateTime fromDate,
            LocalDateTime toDate) {
        
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Complaint> root = query.from(Complaint.class);
        
        // Build predicates dynamically
        List<Predicate> predicates = buildCommonPredicates(cb, root, agency, fromDate, toDate);
        
        // Add status predicate (required)
        predicates.add(cb.equal(root.get("status"), status));
        
        query.select(cb.count(root))
             .where(predicates.toArray(new Predicate[0]));
        
        Long result = entityManager.createQuery(query).getSingleResult();
        return result != null ? result : 0L;
    }
    
    @Override
    public List<Complaint> findResolvedComplaints(
            ComplaintStatus status,
            GovernmentAgencyType agency,
            LocalDateTime fromDate,
            LocalDateTime toDate) {
        
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Complaint> query = cb.createQuery(Complaint.class);
        Root<Complaint> root = query.from(Complaint.class);
        
        // Build predicates dynamically
        List<Predicate> predicates = buildCommonPredicates(cb, root, agency, fromDate, toDate);
        
        // Add status predicate (required)
        predicates.add(cb.equal(root.get("status"), status));
        
        query.select(root)
             .where(predicates.toArray(new Predicate[0]));
        
        return entityManager.createQuery(query).getResultList();
    }
    
    /**
     * Build common predicates for agency and date range filtering.
     * Only adds predicates for non-null values, avoiding PostgreSQL type inference issues.
     */
    private List<Predicate> buildCommonPredicates(
            CriteriaBuilder cb, 
            Root<Complaint> root,
            GovernmentAgencyType agency,
            LocalDateTime fromDate,
            LocalDateTime toDate) {
        
        List<Predicate> predicates = new ArrayList<>();
        
        if (agency != null) {
            predicates.add(cb.equal(root.get("governmentAgency"), agency));
        }
        
        if (fromDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
        }
        
        if (toDate != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
        }
        
        return predicates;
    }
}
