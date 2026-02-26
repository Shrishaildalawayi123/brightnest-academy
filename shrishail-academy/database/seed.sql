USE shrishail_academy;

-- Fix icon column size
ALTER TABLE courses MODIFY COLUMN icon VARCHAR(50);

-- Insert Admin user (password: admin123)
INSERT INTO users (name, email, password, phone, role)
VALUES ('Admin', 'admin@academy.com', '$2a$10$xQGE5BhYV5YOB.YLhXGze.8FqLHKCKLLPWYVLLqhL5GQpLUzKqz4G', '+91 98765 43210', 'ADMIN')
ON DUPLICATE KEY UPDATE name=name;

-- Insert Test Student (password: student123)
INSERT INTO users (name, email, password, phone, role)
VALUES ('Test Student', 'student@test.com', '$2a$10$N.5Wh4fPPHFDl15f0dN7C.1YMHKqidqrT3hGpFQqiVp0jUq3hDMSi', '+91 98765 43211', 'STUDENT')
ON DUPLICATE KEY UPDATE name=name;

-- Insert Courses
INSERT INTO courses (title, description, duration, icon, color, fee) VALUES
('Mathematics', 'Master mathematical concepts from basics to advanced levels covering algebra, geometry, calculus, and more with practical problem-solving techniques.', '12 months', '📐', '#3B82F6', 3000.00),
('Science', 'Explore Physics, Chemistry, and Biology through interactive learning. We make science fun with experiments and real-world applications.', '12 months', '🔬', '#10B981', 3500.00),
('English', 'Develop strong communication skills with our comprehensive English program covering grammar, literature, writing, and spoken English.', '10 months', '📚', '#8B5CF6', 2500.00),
('Kannada', 'Learn Karnataka beautiful language with focus on reading, writing, and literature. Perfect for students preparing for board exams.', '8 months', 'Ka', '#F59E0B', 2000.00),
('Hindi', 'Master Hindi language skills through comprehensive lessons in grammar, literature, and composition. Ideal for all proficiency levels.', '10 months', 'Hi', '#EF4444', 2500.00),
('Sanskrit', 'Discover the ancient language of Sanskrit with expert guidance in grammar, slokas, and classical texts.', '12 months', 'Sa', '#EC4899', 2000.00),
('French', 'Learn French with interactive sessions covering conversation, grammar, and French culture. DELF exam preparation available.', '14 months', '🇫🇷', '#06B6D4', 4000.00)
ON DUPLICATE KEY UPDATE title=title;

-- Sample enrollment
INSERT INTO enrollments (user_id, course_id, status)
SELECT u.id, c.id, 'ACTIVE'
FROM users u, courses c
WHERE u.email = 'student@test.com' AND c.title = 'Mathematics'
LIMIT 1
ON DUPLICATE KEY UPDATE status=status;

-- Confirm
SELECT 'Users:' as Info, COUNT(*) as Count FROM users
UNION ALL
SELECT 'Courses:', COUNT(*) FROM courses
UNION ALL
SELECT 'Enrollments:', COUNT(*) FROM enrollments;
