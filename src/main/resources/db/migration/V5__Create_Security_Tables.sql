-- V5__Create_Security_Tables.sql
-- Creates tables for JWT authentication and user management
-- Author: Security Agent 1
-- Date: 2025-10-23

-- Create users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    password_changed_at TIMESTAMP
);

-- Create indexes for users table
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_enabled ON users(enabled);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Create roles table
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Create index for roles table
CREATE UNIQUE INDEX idx_roles_name ON roles(name);

-- Create user_roles junction table
CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Create indexes for user_roles table
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- Create role_permissions table
CREATE TABLE role_permissions (
    role_id UUID NOT NULL,
    permission VARCHAR(100) NOT NULL,
    PRIMARY KEY (role_id, permission),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Create index for role_permissions table
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);

-- Insert default roles
INSERT INTO roles (id, name, description) VALUES
    ('550e8400-e29b-41d4-a716-446655440001', 'ROLE_USER', 'Standard user role with basic access'),
    ('550e8400-e29b-41d4-a716-446655440002', 'ROLE_ADMIN', 'Administrator role with full access'),
    ('550e8400-e29b-41d4-a716-446655440003', 'ROLE_STAFF', 'Staff role with operational access');

-- Insert default permissions for ROLE_USER
INSERT INTO role_permissions (role_id, permission) VALUES
    ('550e8400-e29b-41d4-a716-446655440001', 'RESERVATION_CREATE'),
    ('550e8400-e29b-41d4-a716-446655440001', 'RESERVATION_READ'),
    ('550e8400-e29b-41d4-a716-446655440001', 'RESERVATION_UPDATE'),
    ('550e8400-e29b-41d4-a716-446655440001', 'RESERVATION_CANCEL'),
    ('550e8400-e29b-41d4-a716-446655440001', 'PAYMENT_CREATE'),
    ('550e8400-e29b-41d4-a716-446655440001', 'PAYMENT_READ'),
    ('550e8400-e29b-41d4-a716-446655440001', 'LOYALTY_READ'),
    ('550e8400-e29b-41d4-a716-446655440001', 'LOYALTY_UPDATE');

-- Insert default permissions for ROLE_ADMIN
INSERT INTO role_permissions (role_id, permission) VALUES
    ('550e8400-e29b-41d4-a716-446655440002', 'RESERVATION_CREATE'),
    ('550e8400-e29b-41d4-a716-446655440002', 'RESERVATION_READ'),
    ('550e8400-e29b-41d4-a716-446655440002', 'RESERVATION_UPDATE'),
    ('550e8400-e29b-41d4-a716-446655440002', 'RESERVATION_CANCEL'),
    ('550e8400-e29b-41d4-a716-446655440002', 'RESERVATION_DELETE'),
    ('550e8400-e29b-41d4-a716-446655440002', 'PAYMENT_CREATE'),
    ('550e8400-e29b-41d4-a716-446655440002', 'PAYMENT_READ'),
    ('550e8400-e29b-41d4-a716-446655440002', 'PAYMENT_UPDATE'),
    ('550e8400-e29b-41d4-a716-446655440002', 'PAYMENT_REFUND'),
    ('550e8400-e29b-41d4-a716-446655440002', 'LOYALTY_READ'),
    ('550e8400-e29b-41d4-a716-446655440002', 'LOYALTY_UPDATE'),
    ('550e8400-e29b-41d4-a716-446655440002', 'LOYALTY_DELETE'),
    ('550e8400-e29b-41d4-a716-446655440002', 'REPORT_READ'),
    ('550e8400-e29b-41d4-a716-446655440002', 'REPORT_GENERATE'),
    ('550e8400-e29b-41d4-a716-446655440002', 'INVENTORY_READ'),
    ('550e8400-e29b-41d4-a716-446655440002', 'INVENTORY_UPDATE'),
    ('550e8400-e29b-41d4-a716-446655440002', 'PRICING_READ'),
    ('550e8400-e29b-41d4-a716-446655440002', 'PRICING_UPDATE'),
    ('550e8400-e29b-41d4-a716-446655440002', 'USER_MANAGEMENT');

-- Insert default permissions for ROLE_STAFF
INSERT INTO role_permissions (role_id, permission) VALUES
    ('550e8400-e29b-41d4-a716-446655440003', 'RESERVATION_CREATE'),
    ('550e8400-e29b-41d4-a716-446655440003', 'RESERVATION_READ'),
    ('550e8400-e29b-41d4-a716-446655440003', 'RESERVATION_UPDATE'),
    ('550e8400-e29b-41d4-a716-446655440003', 'PAYMENT_CREATE'),
    ('550e8400-e29b-41d4-a716-446655440003', 'PAYMENT_READ'),
    ('550e8400-e29b-41d4-a716-446655440003', 'INVENTORY_READ');

-- Create default admin user
-- Password: Admin@123 (BCrypt hash with strength 12)
-- IMPORTANT: Change this password immediately after first deployment
INSERT INTO users (id, username, email, password_hash, first_name, last_name, enabled, created_at, password_changed_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440100',
    'admin',
    'admin@westbethel.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIqKDEZe5e',
    'System',
    'Administrator',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Assign ROLE_ADMIN to default admin user
INSERT INTO user_roles (user_id, role_id)
VALUES ('550e8400-e29b-41d4-a716-446655440100', '550e8400-e29b-41d4-a716-446655440002');

-- Add comments to tables
COMMENT ON TABLE users IS 'Stores user account information for authentication and authorization';
COMMENT ON TABLE roles IS 'Defines roles that can be assigned to users';
COMMENT ON TABLE user_roles IS 'Junction table linking users to their assigned roles';
COMMENT ON TABLE role_permissions IS 'Defines permissions associated with each role';

-- Add comments to important columns
COMMENT ON COLUMN users.password_hash IS 'BCrypt hashed password (strength 12)';
COMMENT ON COLUMN users.failed_login_attempts IS 'Counter for failed login attempts (locks account at 5)';
COMMENT ON COLUMN users.locked_until IS 'Timestamp until which the account is locked';
COMMENT ON COLUMN users.password_changed_at IS 'Timestamp of last password change';
