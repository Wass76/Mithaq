package com.Shakwa.complaint.Enum;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Status of an information request
 */
public enum InformationRequestStatus {
    
    /**
     * Request is pending - waiting for citizen response
     */
    PENDING,
    
    /**
     * Citizen has provided the requested information
     */
    RESPONDED,
    
    /**
     * Request was cancelled by employee
     */
    CANCELLED;

    @JsonValue
    public String getLabel() {
        return this.name().replace("_", " ");
    }
}

