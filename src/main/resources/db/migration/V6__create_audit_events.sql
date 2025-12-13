-- Migration: Create audit_events table
-- Feature: System-wide Audit Logging
-- Description: Creates audit_events table to track all system operations for security and compliance
-- Note: This is separate from complaint_history which tracks complaint-specific changes with detailed field information

-- Create sequence
CREATE SEQUENCE IF NOT EXISTS audit_event_id_seq START WITH 1 INCREMENT BY 1;

-- Create audit_events table
CREATE TABLE IF NOT EXISTS audit_events (
    id BIGINT PRIMARY KEY DEFAULT nextval('audit_event_id_seq'),
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT,
    actor_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    details TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT NOT NULL,
    last_modified_by BIGINT,
    created_by_user_type VARCHAR(50),
    last_modified_by_user_type VARCHAR(50),
    
    CONSTRAINT fk_audit_events_actor 
        FOREIGN KEY (actor_id) REFERENCES users(id) 
        ON DELETE RESTRICT
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_audit_events_actor 
    ON audit_events(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_action 
    ON audit_events(action);
CREATE INDEX IF NOT EXISTS idx_audit_events_target 
    ON audit_events(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_created_at 
    ON audit_events(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_events_status 
    ON audit_events(status);

-- Add comments for documentation
COMMENT ON TABLE audit_events IS 'System-wide audit log for all operations (separate from complaint_history)';
COMMENT ON COLUMN audit_events.action IS 'Action performed (e.g., CREATE_COMPLAINT, UPDATE_USER, LOGIN)';
COMMENT ON COLUMN audit_events.target_type IS 'Type of target entity (e.g., COMPLAINT, USER, EMPLOYEE)';
COMMENT ON COLUMN audit_events.target_id IS 'ID of the target entity';
COMMENT ON COLUMN audit_events.actor_id IS 'User who performed the action';
COMMENT ON COLUMN audit_events.status IS 'Status: SUCCESS or FAILURE';
COMMENT ON COLUMN audit_events.details IS 'Additional details in JSON format';
COMMENT ON COLUMN audit_events.ip_address IS 'IP address of the client who performed the action';

