-- FlagForge PostgreSQL User Setup
-- This script creates application-specific users with appropriate permissions.
-- The 'flagforge' superuser owns the database; service users have limited access.

-- Create webapp-flagforge user for the webapp-api service
CREATE
USER "webapp-flagforge" WITH PASSWORD 'webapp-flagforge';

-- Grant connection to the database
GRANT CONNECT
ON DATABASE flagforge TO "webapp-flagforge";

-- Grant usage and create on public schema (CREATE needed for Flyway migrations)
GRANT USAGE, CREATE
ON SCHEMA public TO "webapp-flagforge";

-- Grant all privileges on all tables (current and future)
GRANT ALL PRIVILEGES ON ALL
TABLES IN SCHEMA public TO "webapp-flagforge";
ALTER
DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO "webapp-flagforge";

-- Grant all privileges on all sequences (current and future)
GRANT ALL PRIVILEGES ON ALL
SEQUENCES IN SCHEMA public TO "webapp-flagforge";
ALTER
DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO "webapp-flagforge";

-- Future: Add customer-flagforge user here when needed
-- CREATE USER "customer-flagforge" WITH PASSWORD 'customer-flagforge';
-- GRANT CONNECT ON DATABASE flagforge TO "customer-flagforge";
-- GRANT USAGE ON SCHEMA public TO "customer-flagforge";
-- GRANT SELECT, INSERT, UPDATE ON specific_tables TO "customer-flagforge";
