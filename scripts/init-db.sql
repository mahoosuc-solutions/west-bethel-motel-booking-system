-- Initial Database Setup Script for West Bethel Motel Booking System
-- This script is executed when the PostgreSQL container is first created

-- The database is already created by the POSTGRES_DB environment variable
-- This script can be used for additional initialization if needed

-- Grant all privileges to the application user (already done by default)
-- This is just a placeholder for any custom initialization

-- Create any extensions that might be needed
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Log initialization
SELECT 'Database initialized for West Bethel Motel Booking System' AS status;
