package com.Shakwa.complaint.Enum;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ComplaintStatus {
    
    PENDING,
    IN_PROGRESS,
    INFO_REQUESTED,
    RESOLVED,
    CLOSED,
    REJECTED;

    @JsonValue
    public String getLabel() {
        return this.name().replace("_", " ");
    }
}

