-- V8__Add_MFA_Fields.sql
-- Adds Multi-Factor Authentication (MFA) support with TOTP
-- Author: Security Agent 1 - Phase 2
-- Date: 2025-10-23

-- Add MFA fields to users table
ALTER TABLE users ADD COLUMN mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN mfa_secret VARCHAR(255);
ALTER TABLE users ADD COLUMN mfa_enabled_at TIMESTAMP;

-- Create mfa_backup_codes table for recovery codes
CREATE TABLE mfa_backup_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mfa_backup_codes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for mfa_backup_codes table
CREATE INDEX idx_mfa_backup_codes_user_id ON mfa_backup_codes(user_id);
CREATE INDEX idx_mfa_backup_codes_used ON mfa_backup_codes(used);

-- Create index for users mfa_enabled
CREATE INDEX idx_users_mfa_enabled ON users(mfa_enabled);

-- Add comments
COMMENT ON COLUMN users.mfa_enabled IS 'Whether MFA is enabled for this user';
COMMENT ON COLUMN users.mfa_secret IS 'Encrypted TOTP secret key for MFA';
COMMENT ON COLUMN users.mfa_enabled_at IS 'Timestamp when MFA was enabled';
COMMENT ON TABLE mfa_backup_codes IS 'Stores hashed backup codes for MFA recovery';
COMMENT ON COLUMN mfa_backup_codes.code_hash IS 'BCrypt hashed backup code';
COMMENT ON COLUMN mfa_backup_codes.used IS 'Whether the backup code has been used (single use only)';
