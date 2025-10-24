-- V6__Create_Password_Reset_Tables.sql
-- Creates tables for password reset functionality
-- Author: Security Agent 1 - Phase 2
-- Date: 2025-10-23

-- Create password_reset_tokens table
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for password_reset_tokens table
CREATE INDEX idx_password_reset_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_expires_at ON password_reset_tokens(expires_at);
CREATE INDEX idx_password_reset_used ON password_reset_tokens(used);

-- Add comments
COMMENT ON TABLE password_reset_tokens IS 'Stores password reset tokens with expiry and usage tracking';
COMMENT ON COLUMN password_reset_tokens.token IS 'Secure UUID token for password reset verification';
COMMENT ON COLUMN password_reset_tokens.expires_at IS 'Token expiry timestamp (1 hour from creation)';
COMMENT ON COLUMN password_reset_tokens.used IS 'Whether the token has been used';
COMMENT ON COLUMN password_reset_tokens.ip_address IS 'IP address from which the reset was requested';
