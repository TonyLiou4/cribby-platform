-- ============================================================================
-- Cribby / Roommate Marketplace — local database bootstrap
--
-- Creates the `roommate_marketplace` database used by the "dev" Spring
-- profile (src/main/resources/application-dev.yml):
--
--   spring.datasource.url: jdbc:postgresql://localhost:5432/roommate_marketplace
--
-- Run this ONCE per PostgreSQL install/reinstall, connected to the default
-- `postgres` maintenance database (NOT to roommate_marketplace itself,
-- since it doesn't exist yet):
--
--   psql postgres -f scripts/create-local-database.sql
--
-- CREATE DATABASE cannot run inside a transaction block in PostgreSQL, so
-- this script does not open one (no BEGIN/COMMIT). The guard below uses
-- psql's \gexec meta-command so the script is also safe to re-run: if the
-- database already exists, the generated CREATE DATABASE statement is
-- simply not executed.
-- ============================================================================

SELECT 'CREATE DATABASE roommate_marketplace'
WHERE NOT EXISTS (
    SELECT 1 FROM pg_database WHERE datname = 'roommate_marketplace'
)\gexec
