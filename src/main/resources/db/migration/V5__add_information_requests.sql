-- Migration: Add information requests table
-- Feature: Additional Information Request
-- Description: Creates information_requests table to allow employees to request additional info from citizens

-- Create sequences
CREATE SEQUENCE IF NOT EXISTS information_request_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS information_request_attachment_id_seq START WITH 1 INCREMENT BY 1;

-- Create information_requests table
CREATE TABLE IF NOT EXISTS information_requests (
    id BIGSERIAL PRIMARY KEY,
    complaint_id BIGINT NOT NULL,
    requested_by_id BIGINT NOT NULL,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    request_message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    responded_at TIMESTAMP,
    response_message TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_info_request_complaint 
        FOREIGN KEY (complaint_id) REFERENCES complaints(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_info_request_requester 
        FOREIGN KEY (requested_by_id) REFERENCES users(id) 
        ON DELETE RESTRICT,
    CONSTRAINT chk_info_request_status 
        CHECK (status IN ('PENDING', 'RESPONDED', 'CANCELLED'))
);

-- Create indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_info_request_complaint 
    ON information_requests(complaint_id);
CREATE INDEX IF NOT EXISTS idx_info_request_status 
    ON information_requests(status);
CREATE INDEX IF NOT EXISTS idx_info_request_requested_by 
    ON information_requests(requested_by_id);
CREATE INDEX IF NOT EXISTS idx_info_request_complaint_status 
    ON information_requests(complaint_id, status);

-- Add comments for documentation
COMMENT ON TABLE information_requests IS 'Stores requests from employees to citizens for additional information about complaints';
COMMENT ON COLUMN information_requests.complaint_id IS 'Reference to the complaint';
COMMENT ON COLUMN information_requests.requested_by_id IS 'Employee who requested the information';
COMMENT ON COLUMN information_requests.requested_at IS 'When the request was created';
COMMENT ON COLUMN information_requests.request_message IS 'Message from employee explaining what information is needed';
COMMENT ON COLUMN information_requests.status IS 'Status: PENDING, RESPONDED, or CANCELLED';
COMMENT ON COLUMN information_requests.responded_at IS 'When the citizen provided the information';
COMMENT ON COLUMN information_requests.response_message IS 'Response message from citizen';
COMMENT ON COLUMN information_requests.version IS 'Optimistic locking version';

-- Create junction table for information request attachments
-- This links attachments uploaded in response to an information request
CREATE TABLE IF NOT EXISTS information_request_attachments (
    id BIGSERIAL PRIMARY KEY,
    information_request_id BIGINT NOT NULL,
    attachment_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_info_req_att_request 
        FOREIGN KEY (information_request_id) REFERENCES information_requests(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_info_req_att_attachment 
        FOREIGN KEY (attachment_id) REFERENCES complaint_attachments(id) 
        ON DELETE CASCADE,
    CONSTRAINT uk_info_req_att 
        UNIQUE (information_request_id, attachment_id)
);

-- Create index for junction table
CREATE INDEX IF NOT EXISTS idx_info_req_att_request 
    ON information_request_attachments(information_request_id);
CREATE INDEX IF NOT EXISTS idx_info_req_att_attachment 
    ON information_request_attachments(attachment_id);

COMMENT ON TABLE information_request_attachments IS 'Links complaint attachments to information requests';
COMMENT ON COLUMN information_request_attachments.information_request_id IS 'Reference to the information request';
COMMENT ON COLUMN information_request_attachments.attachment_id IS 'Reference to the complaint attachment';

