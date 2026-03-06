-- =============================================================
-- aws-rds-init.sql — AWS RDS MySQL Setup for BrightNest Academy
--
-- Run ONCE as the RDS master user (e.g., admin) after creating
-- your RDS instance:
--
--   mysql -h <rds-endpoint> -u admin -p < aws-rds-init.sql
--
-- This script:
--   1. Creates the application database
--   2. Creates a least-privilege application user
--   3. Applies the schema (schema.sql)
--   4. Seeds initial data (seed.sql)
-- =============================================================

-- --------------------------------------------------------
-- 1) Database
-- --------------------------------------------------------
CREATE DATABASE IF NOT EXISTS brightnest_academy
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 2) Application user (NOT the master user)
--    Replace 'CHANGE_ME_strong_db_password' with a real
--    password (store it in /opt/brightnest/.env as DB_PASS)
-- --------------------------------------------------------
CREATE USER IF NOT EXISTS 'brightnest_app'@'%'
  IDENTIFIED BY 'CHANGE_ME_strong_db_password';

-- Grant only what the app needs — no SUPER, no GRANT OPTION
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, INDEX, ALTER
  ON brightnest_academy.*
  TO 'brightnest_app'@'%';

FLUSH PRIVILEGES;

-- --------------------------------------------------------
-- 3) Switch to the application database
-- --------------------------------------------------------
USE brightnest_academy;

-- --------------------------------------------------------
-- 4) Schema (inline from database/schema.sql)
--    Run schema.sql directly if you prefer:
--      mysql -h <rds-endpoint> -u brightnest_app -p \
--            brightnest_academy < database/schema.sql
-- --------------------------------------------------------
SOURCE database/schema.sql;

-- --------------------------------------------------------
-- 5) Seed data (inline from database/seed.sql)
-- --------------------------------------------------------
SOURCE database/seed.sql;

-- --------------------------------------------------------
-- 6) Verify
-- --------------------------------------------------------
SELECT CONCAT('Tables created: ', COUNT(*)) AS result
FROM information_schema.tables
WHERE table_schema = 'brightnest_academy';

SELECT CONCAT('User created: ', User, '@', Host) AS result
FROM mysql.user
WHERE User = 'brightnest_app';
