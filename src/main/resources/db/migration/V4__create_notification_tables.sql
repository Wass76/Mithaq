-- Create notification_tokens table
CREATE SEQUENCE IF NOT EXISTS notification_token_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS notification_tokens (
    id BIGINT PRIMARY KEY DEFAULT nextval('notification_token_id_seq'),
    user_id BIGINT NOT NULL,
    token VARCHAR(1000) NOT NULL,
    device_type VARCHAR(50),
    device_info VARCHAR(500),
    last_used_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT NOT NULL,
    last_modified_by BIGINT,
    created_by_user_type VARCHAR(50),
    last_modified_by_user_type VARCHAR(50),
    CONSTRAINT fk_notification_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_notification_token_user_token UNIQUE (user_id, token)
);

CREATE INDEX IF NOT EXISTS idx_notification_tokens_user_id ON notification_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_tokens_is_active ON notification_tokens(is_active);
CREATE INDEX IF NOT EXISTS idx_notification_tokens_user_active ON notification_tokens(user_id, is_active);

-- Create notifications table
CREATE SEQUENCE IF NOT EXISTS notification_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT PRIMARY KEY DEFAULT nextval('notification_id_seq'),
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    data TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    error_message VARCHAR(1000),
    notification_type VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT NOT NULL,
    last_modified_by BIGINT,
    created_by_user_type VARCHAR(50),
    last_modified_by_user_type VARCHAR(50),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications(user_id, read_at);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);
