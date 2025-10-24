-- V7__Add_Email_Verification.sql
-- Adds email verification functionality
-- Author: Security Agent 1 - Phase 2
-- Date: 2025-10-23

-- Add email verification fields to users table
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMP;

-- Create email_verification_tokens table
CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for email_verification_tokens table
CREATE INDEX idx_email_verification_token ON email_verification_tokens(token);
CREATE INDEX idx_email_verification_user_id ON email_verification_tokens(user_id);
CREATE INDEX idx_email_verification_expires_at ON email_verification_tokens(expires_at);
CREATE INDEX idx_email_verification_verified ON email_verification_tokens(verified);

-- Create index for users email_verified
CREATE INDEX idx_users_email_verified ON users(email_verified);

-- Add comments
COMMENT ON COLUMN users.email_verified IS 'Whether the user email address has been verified';
COMMENT ON COLUMN users.email_verified_at IS 'Timestamp when email was verified';
COMMENT ON TABLE email_verification_tokens IS 'Stores email verification tokens with expiry tracking';
COMMENT ON COLUMN email_verification_tokens.token IS 'Secure UUID token for email verification';
COMMENT ON COLUMN email_verification_tokens.expires_at IS 'Token expiry timestamp (24 hours from creation)';
