-- Initialize database for Codespaces development environment
-- This script runs automatically when the PostgreSQL container starts

-- Create additional databases for testing if needed
CREATE DATABASE motel_booking_test;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE motel_booking_dev TO postgres;
GRANT ALL PRIVILEGES ON DATABASE motel_booking_test TO postgres;

-- Enable required extensions
\c motel_booking_dev;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

\c motel_booking_test;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Log successful initialization
\c motel_booking_dev;
DO $$
BEGIN
  RAISE NOTICE 'Database initialization complete for Codespaces environment';
  RAISE NOTICE 'Database: motel_booking_dev';
  RAISE NOTICE 'Test Database: motel_booking_test';
  RAISE NOTICE 'Extensions enabled: uuid-ossp, pg_trgm';
END $$;
