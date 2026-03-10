-- =====================================================
-- Class Scheduling System Schema
-- BrightNest Academy - Scheduling Feature
-- =====================================================

USE shrishail_academy;

-- =====================================================
-- Table: class_schedules
-- Stores recurring class schedule templates
-- =====================================================
CREATE TABLE IF NOT EXISTS class_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL COMMENT 'MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY',
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room_number VARCHAR(50),
    max_students INT DEFAULT 30,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_schedule_tenant (tenant_id),
    INDEX idx_schedule_course (course_id),
    INDEX idx_schedule_teacher (teacher_id),
    INDEX idx_schedule_day (day_of_week),
    INDEX idx_schedule_active (is_active),
    INDEX idx_schedule_teacher_day_time (teacher_id, day_of_week, start_time, end_time),
    
    CONSTRAINT chk_time_order CHECK (end_time > start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Table: class_sessions
-- Stores individual class session instances
-- =====================================================
CREATE TABLE IF NOT EXISTS class_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    session_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'SCHEDULED' COMMENT 'SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED',
    actual_start_time TIME,
    actual_end_time TIME,
    attendance_marked BOOLEAN DEFAULT FALSE,
    notes TEXT,
    cancellation_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    FOREIGN KEY (schedule_id) REFERENCES class_schedules(id) ON DELETE CASCADE,
    
    UNIQUE KEY uk_schedule_date (schedule_id, session_date),
    INDEX idx_session_tenant (tenant_id),
    INDEX idx_session_schedule (schedule_id),
    INDEX idx_session_date (session_date),
    INDEX idx_session_status (status),
    INDEX idx_session_date_status (session_date, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Table: session_attendance
-- Tracks student attendance for each session
-- =====================================================
CREATE TABLE IF NOT EXISTS session_attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'ABSENT' COMMENT 'PRESENT, ABSENT, LATE, EXCUSED',
    check_in_time TIME,
    notes TEXT,
    marked_at TIMESTAMP,
    marked_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    FOREIGN KEY (session_id) REFERENCES class_sessions(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (marked_by) REFERENCES users(id) ON DELETE SET NULL,
    
    UNIQUE KEY uk_session_student (session_id, student_id),
    INDEX idx_attendance_tenant (tenant_id),
    INDEX idx_attendance_session (session_id),
    INDEX idx_attendance_student (student_id),
    INDEX idx_attendance_status (status),
    INDEX idx_attendance_student_status (student_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Table: assignments
-- Stores course assignments/homework
-- =====================================================
CREATE TABLE IF NOT EXISTS assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    due_date TIMESTAMP NOT NULL,
    max_score INT DEFAULT 100,
    attachment_url VARCHAR(500),
    is_published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_assignment_tenant (tenant_id),
    INDEX idx_assignment_course (course_id),
    INDEX idx_assignment_teacher (teacher_id),
    INDEX idx_assignment_due_date (due_date),
    INDEX idx_assignment_published (is_published),
    INDEX idx_assignment_course_due (course_id, due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Table: assignment_submissions
-- Stores student assignment submissions
-- =====================================================
CREATE TABLE IF NOT EXISTS assignment_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    assignment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    submission_text TEXT,
    attachment_url VARCHAR(500),
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    score INT,
    feedback TEXT,
    graded_at TIMESTAMP NULL,
    graded_by BIGINT,
    is_late BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (graded_by) REFERENCES users(id) ON DELETE SET NULL,
    
    UNIQUE KEY uk_student_assignment (student_id, assignment_id),
    INDEX idx_submission_tenant (tenant_id),
    INDEX idx_submission_assignment (assignment_id),
    INDEX idx_submission_student (student_id),
    INDEX idx_submission_graded (graded_at),
    INDEX idx_submission_student_assignment (student_id, assignment_id),
    
    CONSTRAINT chk_score_range CHECK (score IS NULL OR (score >= 0 AND score <= 100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Seed Data: Sample Class Schedule
-- =====================================================

-- Note: Actual seeds will be created by DataInitializer at runtime
-- This is just for reference structure

-- Sample: Mathematics class on Monday and Wednesday
-- INSERT INTO class_schedules (tenant_id, course_id, teacher_id, day_of_week, start_time, end_time, room_number)
-- VALUES 
-- (1, 1, 2, 'MONDAY', '10:00:00', '11:30:00', 'Room 101'),
-- (1, 1, 2, 'WEDNESDAY', '10:00:00', '11:30:00', 'Room 101');
