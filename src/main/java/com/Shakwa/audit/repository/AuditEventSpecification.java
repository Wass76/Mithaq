package com.Shakwa.audit.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.Shakwa.audit.entity.AuditEvent;

import jakarta.persistence.criteria.Predicate;

/**
 * JPA Specifications for dynamic AuditEvent queries.
 * This approach avoids PostgreSQL parameter type inference issues that occur
 * with the traditional (:param IS NULL OR ...) JPQL pattern.
 */
public class AuditEventSpecification {
    
    private AuditEventSpecification() {
        // Utility class
    }
    
    /**
     * Create a specification for filtering audit events with multiple optional criteria.
     * Only non-null parameters will be added as filter conditions.
     *
     * @param actorId Filter by actor ID
     * @param action Filter by action
     * @param targetType Filter by target type
     * @param targetId Filter by target ID
     * @param status Filter by status
     * @param fromDate Filter by start date (inclusive)
     * @param toDate Filter by end date (inclusive)
     * @return Specification for AuditEvent
     */
    public static Specification<AuditEvent> withFilters(
            Long actorId,
            String action,
            String targetType,
            Long targetId,
            String status,
            LocalDateTime fromDate,
            LocalDateTime toDate) {
        
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (actorId != null) {
                predicates.add(criteriaBuilder.equal(root.get("actorId"), actorId));
            }
            
            if (action != null && !action.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("action"), action));
            }
            
            if (targetType != null && !targetType.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("targetType"), targetType));
            }
            
            if (targetId != null) {
                predicates.add(criteriaBuilder.equal(root.get("targetId"), targetId));
            }
            
            if (status != null && !status.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            
            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }
            
            // Default ordering by createdAt descending
            query.orderBy(criteriaBuilder.desc(root.get("createdAt")));
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
