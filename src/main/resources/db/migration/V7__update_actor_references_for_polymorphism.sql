-- Migration: Update actor references to support polymorphic users (User, Citizen, Employee)
-- Feature: Allow Citizens and Employees to be recorded as actors in history and attachments
-- Description: Changes FK references to simple ID columns with actor metadata

-- ============================================
-- Update complaint_history table
-- ============================================

-- Drop the foreign key constraint on actor_id (it referenced users table only)
ALTER TABLE complaint_history 
    DROP CONSTRAINT IF EXISTS fk_complaint_history_actor;

-- Add new columns for actor metadata
ALTER TABLE complaint_history 
    ADD COLUMN IF NOT EXISTS actor_name VARCHAR(255);

ALTER TABLE complaint_history 
    ADD COLUMN IF NOT EXISTS actor_email VARCHAR(255);

ALTER TABLE complaint_history 
    ADD COLUMN IF NOT EXISTS actor_type VARCHAR(50);

-- Update existing records to populate actor metadata from users table
UPDATE complaint_history ch
SET 
    actor_name = COALESCE(u.first_name || ' ' || u.last_name, 'Unknown User'),
    actor_email = u.email,
    actor_type = 'USER'
FROM users u
WHERE ch.actor_id = u.id AND ch.actor_name IS NULL;

-- For records where actor is a citizen (actor_id might reference citizens table)
UPDATE complaint_history ch
SET 
    actor_name = COALESCE(c.first_name || ' ' || c.last_name, ch.actor_name, 'Unknown Citizen'),
    actor_email = COALESCE(c.email, ch.actor_email),
    actor_type = COALESCE(ch.actor_type, 'CITIZEN')
FROM citizens c
WHERE ch.actor_id = c.id AND ch.actor_type IS NULL;

-- Set default values for any remaining NULL values
UPDATE complaint_history 
SET 
    actor_name = 'System',
    actor_type = 'SYSTEM'
WHERE actor_name IS NULL;

-- Make columns not null after populating data
ALTER TABLE complaint_history 
    ALTER COLUMN actor_name SET NOT NULL;

ALTER TABLE complaint_history 
    ALTER COLUMN actor_type SET NOT NULL;

-- ============================================
-- Update complaint_attachments table
-- ============================================

-- Drop the foreign key constraint on uploaded_by if exists
ALTER TABLE complaint_attachments 
    DROP CONSTRAINT IF EXISTS fk_attachment_uploaded_by;

-- Rename uploaded_by to uploaded_by_id if it exists as a column
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'complaint_attachments' AND column_name = 'uploaded_by'
    ) THEN
        ALTER TABLE complaint_attachments RENAME COLUMN uploaded_by TO uploaded_by_id;
    END IF;
END $$;

-- Add uploaded_by_id if it doesn't exist
ALTER TABLE complaint_attachments 
    ADD COLUMN IF NOT EXISTS uploaded_by_id BIGINT;

-- Add new columns for uploader metadata
ALTER TABLE complaint_attachments 
    ADD COLUMN IF NOT EXISTS uploaded_by_name VARCHAR(255);

ALTER TABLE complaint_attachments 
    ADD COLUMN IF NOT EXISTS uploaded_by_type VARCHAR(50);

-- Update existing records to populate uploader metadata from users table
UPDATE complaint_attachments ca
SET 
    uploaded_by_name = COALESCE(u.first_name || ' ' || u.last_name, 'Unknown User'),
    uploaded_by_type = 'USER'
FROM users u
WHERE ca.uploaded_by_id = u.id AND ca.uploaded_by_name IS NULL;

-- For records where uploader is a citizen
UPDATE complaint_attachments ca
SET 
    uploaded_by_name = COALESCE(c.first_name || ' ' || c.last_name, ca.uploaded_by_name, 'Unknown Citizen'),
    uploaded_by_type = COALESCE(ca.uploaded_by_type, 'CITIZEN')
FROM citizens c
WHERE ca.uploaded_by_id = c.id AND ca.uploaded_by_type IS NULL;

-- Set default values for any remaining NULL values
UPDATE complaint_attachments 
SET 
    uploaded_by_name = 'System',
    uploaded_by_type = 'SYSTEM'
WHERE uploaded_by_name IS NULL AND uploaded_by_id IS NOT NULL;

-- Add comments for documentation
COMMENT ON COLUMN complaint_history.actor_id IS 'ID of the user who performed the action (can be User, Citizen, or Employee)';
COMMENT ON COLUMN complaint_history.actor_name IS 'Full name of the actor (stored for display)';
COMMENT ON COLUMN complaint_history.actor_email IS 'Email of the actor';
COMMENT ON COLUMN complaint_history.actor_type IS 'Type of actor: USER, CITIZEN, or EMPLOYEE';

COMMENT ON COLUMN complaint_attachments.uploaded_by_id IS 'ID of the user who uploaded the attachment';
COMMENT ON COLUMN complaint_attachments.uploaded_by_name IS 'Full name of the uploader (stored for display)';
COMMENT ON COLUMN complaint_attachments.uploaded_by_type IS 'Type of uploader: USER, CITIZEN, or EMPLOYEE';

-- ============================================
-- Update notifications table
-- ============================================

-- Drop the foreign key constraint on user_id if exists
ALTER TABLE notifications 
    DROP CONSTRAINT IF EXISTS fk_notification_user;

-- Add user_type column
ALTER TABLE notifications 
    ADD COLUMN IF NOT EXISTS user_type VARCHAR(50);

-- Update existing records to set user_type
UPDATE notifications SET user_type = 'USER' WHERE user_type IS NULL;

-- Make user_type not null
ALTER TABLE notifications 
    ALTER COLUMN user_type SET NOT NULL;

-- ============================================
-- Update notification_tokens table
-- ============================================

-- Drop the foreign key constraint on user_id if exists
ALTER TABLE notification_tokens 
    DROP CONSTRAINT IF EXISTS fk_notification_token_user;

-- Add user_type column
ALTER TABLE notification_tokens 
    ADD COLUMN IF NOT EXISTS user_type VARCHAR(50);

-- Update existing records to set user_type
UPDATE notification_tokens SET user_type = 'USER' WHERE user_type IS NULL;

-- Make user_type not null
ALTER TABLE notification_tokens 
    ALTER COLUMN user_type SET NOT NULL;

-- Update unique constraint
ALTER TABLE notification_tokens 
    DROP CONSTRAINT IF EXISTS notification_tokens_user_id_token_key;

ALTER TABLE notification_tokens 
    ADD CONSTRAINT notification_tokens_user_id_user_type_token_key UNIQUE (user_id, user_type, token);

-- Add comments
COMMENT ON COLUMN notifications.user_id IS 'ID of the user to receive the notification';
COMMENT ON COLUMN notifications.user_type IS 'Type of user: USER, CITIZEN, or EMPLOYEE';

COMMENT ON COLUMN notification_tokens.user_id IS 'ID of the user who owns this token';
COMMENT ON COLUMN notification_tokens.user_type IS 'Type of user: USER, CITIZEN, or EMPLOYEE';
