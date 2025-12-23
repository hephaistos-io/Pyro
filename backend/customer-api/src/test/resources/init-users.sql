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

-- Create customer-flagforge user for the customer-api service
CREATE
    USER "customer-flagforge" WITH PASSWORD 'customer-flagforge';

-- Grant connection to the database
GRANT CONNECT
    ON DATABASE flagforge TO "customer-flagforge";

-- Grant usage on public schema (NO CREATE - webapp-api owns migrations)
GRANT USAGE ON SCHEMA
    public TO "customer-flagforge";

-- Grant SELECT on tables needed for reading flags and validating keys
-- Note: This grants on existing tables; new tables need DEFAULT PRIVILEGES
GRANT
    SELECT
    ON ALL TABLES IN SCHEMA public TO "customer-flagforge";

-- Grant SELECT on sequences (needed for JPA/Hibernate operations)
GRANT
    SELECT
    ON ALL SEQUENCES IN SCHEMA public TO "customer-flagforge";

-- Grant privileges on future tables/sequences created by webapp-flagforge
ALTER
    DEFAULT PRIVILEGES FOR USER "webapp-flagforge" IN SCHEMA public
    GRANT
    SELECT
    ON TABLES TO "customer-flagforge";
ALTER
    DEFAULT PRIVILEGES FOR USER "webapp-flagforge" IN SCHEMA public
    GRANT
    SELECT
    ON SEQUENCES TO "customer-flagforge";
