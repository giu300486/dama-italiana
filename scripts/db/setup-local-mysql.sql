-- =============================================================================
-- Dama Italiana — Local MySQL setup (dev + it profiles)
-- -----------------------------------------------------------------------------
-- Idempotent: safe to re-run. Creates 2 schemas + 1 app user for the `server`
-- module's `dev` (interactive runtime) and `it` (integration tests with
-- flyway:clean) Spring profiles.
--
-- See ARCHITECTURE.md ADR-049 + plans/PLAN-fase-5.md §4 Task 5.3.bis.
--
-- Defaults are intended for *single-developer local* use only. For production
-- deployment see README.md §1.b (different DB name + secure secret via env var;
-- DO NOT run this script in production).
--
-- Run as MySQL admin (e.g. root):
--   mysql -u root -p < scripts/db/setup-local-mysql.sql
-- On Windows full path:
--   "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p ^
--     < scripts/db/setup-local-mysql.sql
--
-- Override defaults by editing the values below before running, or pass the
-- script through `sed` if you script the customization.
-- =============================================================================

-- ── Schemas ──────────────────────────────────────────────────────────────────
-- Two distinct databases on the same MySQL server instance:
--   dama_italiana_dev : interactive runtime DB (profile `dev`). Data persists
--                       across server restarts; managed manually via Workbench
--                       or via the running server (curl / future client).
--   dama_italiana_it  : integration test DB (profile `it`, @Tag("slow")).
--                       flyway.clean() + flyway.migrate() wipes & rebuilds the
--                       schema before each test class — data is volatile.
--
-- Schema (tables, columns, constraints) is IDENTICAL across both DBs because
-- both are managed by the same Flyway V1 migration. They differ ONLY in
-- whether the data is preserved or wiped.

CREATE DATABASE IF NOT EXISTS dama_italiana_dev
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS dama_italiana_it
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- ── App user ─────────────────────────────────────────────────────────────────
-- Single user 'dama'@'localhost' with full privileges on BOTH databases.
-- For production: a different user, restricted to the single prod DB, with
-- a strong password from env var (see README.md §1.b).

CREATE USER IF NOT EXISTS 'dama'@'localhost' IDENTIFIED BY 'dama';

GRANT ALL PRIVILEGES ON dama_italiana_dev.* TO 'dama'@'localhost';
GRANT ALL PRIVILEGES ON dama_italiana_it.*  TO 'dama'@'localhost';

FLUSH PRIVILEGES;

-- ── Verification ─────────────────────────────────────────────────────────────
-- Prints a one-row report when run via the `mysql` CLI.

SELECT
  'OK setup-local-mysql.sql complete' AS status,
  (
    SELECT COUNT(*)
    FROM information_schema.schemata
    WHERE schema_name IN ('dama_italiana_dev', 'dama_italiana_it')
  ) AS schemas_present,
  (
    SELECT COUNT(*)
    FROM mysql.user
    WHERE User = 'dama' AND Host = 'localhost'
  ) AS app_user_present;
