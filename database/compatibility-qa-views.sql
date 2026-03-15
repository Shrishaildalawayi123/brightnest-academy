-- =====================================================
-- Compatibility Views for QA & Legacy Queries
-- Target DB: brightnest
-- =====================================================

USE brightnest;

-- -----------------------------------------------------
-- 1) career_teacher_applications compatibility
-- Maps legacy expected column names to teacher_applications
-- -----------------------------------------------------
CREATE OR REPLACE VIEW career_teacher_applications AS
SELECT
    id,
    tenant_id,
    full_name AS name,
    email,
    phone,
    subject_expertise AS subject,
    qualification,
    city,
    teaching_mode,
    experience,
    motivation AS message,
    resume_file_name,
    resume_file_path,
    status,
    created_at
FROM teacher_applications;

-- -----------------------------------------------------
-- 2) visitor_logs compatibility
-- If visitor_logs table is absent, map to site_visits
-- -----------------------------------------------------
DROP PROCEDURE IF EXISTS ensure_visitor_logs_compat;
DELIMITER $$
CREATE PROCEDURE ensure_visitor_logs_compat()
BEGIN
    DECLARE visitor_logs_table_exists INT DEFAULT 0;
    DECLARE visitor_logs_view_exists INT DEFAULT 0;
    DECLARE site_visits_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO visitor_logs_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'visitor_logs'
      AND table_type = 'BASE TABLE';

    SELECT COUNT(*) INTO visitor_logs_view_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'visitor_logs'
      AND table_type = 'VIEW';

    SELECT COUNT(*) INTO site_visits_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'site_visits'
      AND table_type = 'BASE TABLE';

    -- Do nothing if a real visitor_logs table already exists.
    IF visitor_logs_table_exists = 0 AND site_visits_exists = 1 THEN
        IF visitor_logs_view_exists = 1 THEN
            DROP VIEW visitor_logs;
        END IF;

        SET @sql = 'CREATE VIEW visitor_logs AS
                    SELECT
                        id,
                        session_id,
                        page_url,
                        referrer,
                        ip_address,
                        user_agent,
                        visited_at AS created_at
                    FROM site_visits';
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL ensure_visitor_logs_compat();
DROP PROCEDURE IF EXISTS ensure_visitor_logs_compat;








SELECT * FROM career_teacher_applications ORDER BY created_at DESC LIMIT 5;USE brightnest;-- =====================================================-- Test Query-- =====================================================-- =====================================================
-- Summary of Data in the Database
-- =====================================================

SELECT
(SELECT COUNT(*) FROM contact_messages) AS contact_messages,
(SELECT COUNT(*) FROM teacher_applications) AS teacher_applications,
(SELECT COUNT(*) FROM users WHERE role='STUDENT') AS students,
(SELECT COUNT(*) FROM users WHERE role='TEACHER') AS teachers,
(SELECT COUNT(*) FROM courses) AS courses,
(SELECT COUNT(*) FROM enrollments) AS enrollments,
(SELECT COUNT(*) FROM visitor_logs) AS visitors;
