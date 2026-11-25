-- =============================================================================
-- Dama Italiana — Local MySQL teardown
-- -----------------------------------------------------------------------------
-- Reverses scripts/db/setup-local-mysql.sql. Drops both local DBs and the
-- 'dama'@'localhost' user. Idempotent (IF EXISTS).
--
-- DESTRUCTIVE: this DELETES all data in dama_italiana_dev and dama_italiana_it.
-- Use only to reset to a clean state (e.g. before re-running setup with
-- different credentials, or to free disk after F5 demo).
--
-- Run as MySQL admin (e.g. root):
--   mysql -u root -p < scripts/db/teardown-local-mysql.sql
-- On Windows full path:
--   "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p ^
--     < scripts/db/teardown-local-mysql.sql
-- =============================================================================

DROP DATABASE IF EXISTS dama_italiana_dev;
DROP DATABASE IF EXISTS dama_italiana_it;

DROP USER IF EXISTS 'dama'@'localhost';

FLUSH PRIVILEGES;

SELECT
  'OK teardown-local-mysql.sql complete' AS status,
  (
    SELECT COUNT(*)
    FROM information_schema.schemata
    WHERE schema_name IN ('dama_italiana_dev', 'dama_italiana_it')
  ) AS schemas_still_present,
  (
    SELECT COUNT(*)
    FROM mysql.user
    WHERE User = 'dama' AND Host = 'localhost'
  ) AS app_user_still_present;
