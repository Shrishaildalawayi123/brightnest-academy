-- =====================================================
-- Shrishail Academy Database Schema
-- =====================================================

-- Create database
CREATE DATABASE IF NOT EXISTS shrishail_academy;
USE shrishail_academy;

-- =====================================================
-- Table: users
-- Stores both students and admins
-- =====================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_role (role)
);

-- =====================================================
-- Table: courses
-- Stores all available courses
-- =====================================================
CREATE TABLE IF NOT EXISTS courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    duration VARCHAR(50),
    icon VARCHAR(50),
    color VARCHAR(20),
    fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- =====================================================
-- Table: enrollments
-- Tracks student course enrollments
-- =====================================================
CREATE TABLE IF NOT EXISTS enrollments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    UNIQUE KEY unique_enrollment (user_id, course_id),
    INDEX idx_user_id (user_id),
    INDEX idx_course_id (course_id)
);

-- =====================================================
-- Insert Default Admin User
-- Password: admin123 (BCrypt encoded)
-- =====================================================
INSERT INTO users (name, email, password, phone, role) 
VALUES (
    'Admin', 
    'admin@academy.com', 
    '$2a$10$xQGE5BhYV5YOB.YLhXGze.8FqLHKCKLLPWYVLLqhL5GQpLUzKqz4G',
    '+91 98765 43210',
    'ADMIN'
) ON DUPLICATE KEY UPDATE email=email;

-- =====================================================
-- Insert Default Courses
-- =====================================================
INSERT INTO courses (title, description, duration, icon, color, fee) VALUES
(
    'Mathematics',
    'Master mathematical concepts from basics to advanced levels. Our structured curriculum covers algebra, geometry, calculus, and more with practical problem-solving techniques.',
    '12 months',
    '📐',
    '#3B82F6',
    3000.00
),
(
    'Science',
    'Explore the wonders of Physics, Chemistry, and Biology through interactive learning. We make science fun with experiments and real-world applications.',
    '12 months',
    '🔬',
    '#10B981',
    3500.00
),
(
    'English',
    'Develop strong communication skills with our comprehensive English program covering grammar, literature, writing, and spoken English.',
    '10 months',
    '📚',
    '#8B5CF6',
    2500.00
),
(
    'Kannada',
    'Learn Karnataka''s beautiful language with focus on reading, writing, and literature. Perfect for students preparing for board exams.',
    '8 months',
    'ಕ',
    '#F59E0B',
    2000.00
),
(
    'Hindi',
    'Master Hindi language skills through comprehensive lessons in grammar, literature, and composition. Ideal for all proficiency levels.',
    '10 months',
    'ह',
    '#EF4444',
    2500.00
),
(
    'Sanskrit',
    'Discover the ancient language of Sanskrit with expert guidance in grammar, slokas, and classical texts.',
    '12 months',
    'संस्',
    '#EC4899',
    2000.00
),
(
    'French',
    'Learn French language with interactive sessions covering conversation, grammar, and French culture. DELF exam preparation available.',
    '14 months',
    '🇫🇷',
    '#06B6D4',
    4000.00
)
ON DUPLICATE KEY UPDATE title=title;

-- =====================================================
-- Table: attendance
-- Tracks student attendance per course per date
-- =====================================================
CREATE TABLE IF NOT EXISTS attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    remarks VARCHAR(255),
    marked_by_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    FOREIGN KEY (marked_by_id) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE KEY unique_attendance (user_id, course_id, attendance_date),
    INDEX idx_attendance_user (user_id),
    INDEX idx_attendance_course (course_id),
    INDEX idx_attendance_date (attendance_date)
);

-- =====================================================
-- Table: payments
-- Tracks fee payments for course enrollments
-- =====================================================
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    enrollment_id BIGINT,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL DEFAULT 'UPI',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_id VARCHAR(100) UNIQUE,
    gateway_order_id VARCHAR(100),
    gateway_payment_id VARCHAR(100),
    receipt_number VARCHAR(50) UNIQUE,
    remarks TEXT,
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE RESTRICT,
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(id) ON DELETE SET NULL,
    CHECK (amount > 0),
    INDEX idx_payment_user (user_id),
    INDEX idx_payment_course (course_id),
    INDEX idx_payment_status (status),
    INDEX idx_payment_receipt (receipt_number)
);

-- =====================================================
-- Table: contact_messages
-- Stores contact form submissions from visitors
-- =====================================================
CREATE TABLE IF NOT EXISTS contact_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    subject VARCHAR(200) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_contact_status (status),
    INDEX idx_contact_date (created_at)
);

-- =====================================================
-- Table: testimonials
-- Student reviews displayed on website
-- =====================================================
CREATE TABLE IF NOT EXISTS testimonials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_name VARCHAR(100) NOT NULL,
    course_name VARCHAR(100),
    review VARCHAR(1000) NOT NULL,
    rating INT NOT NULL DEFAULT 5,
    approved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_testimonial_approved (approved)
);

-- =====================================================
-- Sample Student User for Testing
-- Password: student123
-- =====================================================
INSERT INTO users (name, email, password, phone, role) 
VALUES (
    'Test Student', 
    'student@test.com', 
    '$2a$10$8c5qW7mLJh5QlYmZ1YsXxOYfHJ3vQKZ7kLpYvXqZ7qYvXqZ7qYvXq',
    '+91 98765 43211',
    'STUDENT'
) ON DUPLICATE KEY UPDATE email=email;

-- =====================================================
-- Sample Enrollment for Testing
-- =====================================================
INSERT INTO enrollments (user_id, course_id, status)
SELECT u.id, c.id, 'ACTIVE'
FROM users u, courses c
WHERE u.email = 'student@test.com' 
  AND c.title = 'Mathematics'
LIMIT 1
ON DUPLICATE KEY UPDATE status=status;

-- =====================================================
-- Table: blog_posts
-- CMS blog articles for the academy website
-- =====================================================
CREATE TABLE IF NOT EXISTS blog_posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL UNIQUE,
    excerpt VARCHAR(500),
    content LONGTEXT,
    category VARCHAR(40) NOT NULL DEFAULT 'ACADEMY_NEWS',
    cover_image_url VARCHAR(500),
    author VARCHAR(100),
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_blog_slug (slug),
    INDEX idx_blog_category (category),
    INDEX idx_blog_published (published),
    INDEX idx_blog_published_at (published_at)
);

-- =====================================================
-- Table: demo_bookings
-- Demo class booking requests from prospective students
-- =====================================================
CREATE TABLE IF NOT EXISTS demo_bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_name VARCHAR(100) NOT NULL,
    parent_name VARCHAR(100),
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    subject VARCHAR(50) NOT NULL,
    grade VARCHAR(30),
    board VARCHAR(30),
    class_mode VARCHAR(20) NOT NULL DEFAULT 'ONLINE',
    requirements VARCHAR(500),
    message VARCHAR(1000),
    demo_fee INT NOT NULL DEFAULT 100,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    scheduled_date TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_demo_status (status),
    INDEX idx_demo_date (created_at)
);

-- =====================================================
-- Table: teacher_applications
-- Educator recruitment form submissions
-- =====================================================
CREATE TABLE IF NOT EXISTS teacher_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    subject_expertise VARCHAR(200) NOT NULL,
    experience VARCHAR(500),
    motivation VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_teacher_app_status (status),
    INDEX idx_teacher_app_date (created_at)
);

-- =====================================================
-- Seed Blog Posts
-- =====================================================
INSERT INTO blog_posts (title, slug, excerpt, content, category, author, published, published_at) VALUES
(
    '5 Proven Techniques to Master Any Language Faster',
    '5-proven-techniques-to-master-any-language-faster',
    'Discover science-backed strategies that can accelerate your language learning journey and help you achieve fluency in less time.',
    '<h2>Why Traditional Methods Fall Short</h2><p>Many students spend years studying a language through textbooks alone, only to struggle with real conversations. The key to faster fluency lies in combining multiple learning approaches that engage different parts of your brain.</p><h2>1. Spaced Repetition</h2><p>Instead of cramming vocabulary in one sitting, space out your review sessions. Research shows that reviewing material at increasing intervals dramatically improves long-term retention. Start by reviewing new words after 1 day, then 3 days, then a week.</p><h2>2. Immersive Listening</h2><p>Surround yourself with the language through music, podcasts, and audio content. Even passive listening helps your brain recognize patterns, pronunciation, and natural speech rhythms.</p><h2>3. Active Writing Practice</h2><p>Writing forces you to recall vocabulary and grammar rules actively. Start a daily journal in your target language — even just three sentences about your day can make a significant difference.</p><h2>4. Conversational Practice</h2><p>Speaking with others is irreplaceable. Join study groups, work with tutors, or find language exchange partners. The discomfort of making mistakes is actually when the most learning happens.</p><h2>5. Connect with Culture</h2><p>Language exists within a cultural context. Reading literature, watching films, and understanding traditions related to the language gives your learning deeper roots and motivation.</p><h3>Getting Started</h3><p>At BrightNest Academy, our tutors integrate all these techniques into personalized lesson plans. Whether you are learning Sanskrit, Hindi, Kannada, English, or French — our approach ensures faster progress with lasting results.</p>',
    'LEARNING_TIPS',
    'BrightNest Academy',
    TRUE,
    '2026-02-20 10:00:00'
),
(
    'How to Prepare for Board Exams: A Complete Study Guide',
    'how-to-prepare-for-board-exams-complete-study-guide',
    'Board exams can feel overwhelming, but with the right strategy and preparation timeline, students can approach them with confidence and clarity.',
    '<h2>Start Early, Stay Consistent</h2><p>The biggest mistake students make is waiting until the last minute. Ideally, begin your focused preparation at least 3 months before exams. Create a realistic timetable that covers all subjects without burnout.</p><h2>Understand the Syllabus Thoroughly</h2><p>Before diving into study material, carefully review the official syllabus and exam pattern. Identify which topics carry the most marks and prioritize accordingly.</p><h2>Previous Year Papers Are Gold</h2><p>Solving past exam papers is one of the most effective preparation strategies. It helps you understand question patterns, time management, and frequently tested concepts.</p><h2>Make Revision Notes</h2><p>As you study each chapter, create concise revision notes in your own words. These become invaluable during the final weeks when you need quick refreshers.</p><h2>Take Care of Your Health</h2><p>Regular sleep, nutritious food, and short exercise breaks are not luxuries during exam season — they are necessities. A well-rested brain learns and recalls better than an exhausted one.</p><h2>Seek Help When Stuck</h2><p>Do not waste hours struggling with a concept alone. Ask your teachers, tutors, or classmates for help. At BrightNest Academy, our tutors are always available to clear doubts and provide targeted practice.</p><h3>You Have Got This!</h3><p>Remember, board exams test your understanding, not just your memory. Focus on concepts, practice regularly, and believe in your preparation.</p>',
    'EXAM_STRATEGIES',
    'BrightNest Academy',
    TRUE,
    '2026-02-15 09:00:00'
),
(
    'The Beauty of Sanskrit: Why This Ancient Language Matters Today',
    'the-beauty-of-sanskrit-why-this-ancient-language-matters-today',
    'Sanskrit is far more than an ancient language — it is a window into philosophy, science, and literature that continues to influence modern thought and education.',
    '<h2>A Language of Precision</h2><p>Sanskrit is often called the most scientific language in the world. Its grammar, codified by Panini over 2,500 years ago, is so precise and logical that it has been compared to modern programming languages.</p><h2>Academic Benefits</h2><p>Students who study Sanskrit often find it easier to learn other Indo-European languages. The structured grammar training strengthens analytical thinking and improves performance across subjects.</p><h2>CBSE and State Board Relevance</h2><p>For students in grades VI through X, Sanskrit offers a strategic advantage as a second or third language. The scoring potential is high, and the syllabus is well-structured with clear learning outcomes.</p><h2>Beyond the Classroom</h2><p>Sanskrit opens doors to a vast ocean of classical literature, including the Vedas, Upanishads, Ramayana, Mahabharata, and works of Kalidasa. Understanding these texts in their original language provides insights that translations simply cannot capture.</p><h2>Modern Applications</h2><p>From yoga and meditation terminology to Ayurvedic medicine and Indian classical arts — Sanskrit remains a living, breathing part of contemporary life. Its study enriches both mind and spirit.</p><h3>Learn Sanskrit with Us</h3><p>At BrightNest Academy, our Sanskrit tutors bring this ancient language to life with engaging lessons tailored to both academic requirements and genuine appreciation.</p>',
    'LANGUAGE_INSIGHTS',
    'BrightNest Academy',
    TRUE,
    '2026-02-10 11:30:00'
),
(
    'Welcome to BrightNest Academy: Our Story and Mission',
    'welcome-to-brightnest-academy-our-story-and-mission',
    'Learn about BrightNest Academy''s journey, our commitment to quality education, and how we are making a difference in students'' lives across Bangalore.',
    '<h2>How It All Began</h2><p>BrightNest Academy was born from a simple belief: every student deserves access to quality, personalized education. What started as a small tutoring initiative has grown into a comprehensive learning center serving hundreds of students.</p><h2>Our Mission</h2><p>We strive to build confidence in every learner through expert guidance, personalized attention, and a supportive learning environment. Education is not just about grades — it is about nurturing curiosity, discipline, and a love for knowledge.</p><h2>What Makes Us Different</h2><p>Our team of experienced educators brings decades of combined teaching experience. We do not believe in one-size-fits-all education. Every student gets a customized learning path based on their strengths, areas for improvement, and goals.</p><h2>Our Subjects</h2><p>We offer expert tuition in Sanskrit, Hindi, English, Kannada, and French — covering school curriculum, spoken language, and reading/writing proficiency programs. From CBSE and ICSE to State Board syllabi, we have got you covered.</p><h2>Online and Offline Options</h2><p>Whether you prefer the convenience of online classes via Google Meet or the hands-on experience of our physical centers in Bangalore, we offer flexible options to suit every family.</p><h3>Join the BrightNest Family</h3><p>We invite you to experience the difference that dedicated, passionate teaching can make. Book a demo class today and see for yourself!</p>',
    'ACADEMY_NEWS',
    'BrightNest Academy',
    TRUE,
    '2026-01-25 08:00:00'
),
(
    'Choosing Between Hindi and Sanskrit for CBSE: A Parent''s Guide',
    'choosing-between-hindi-and-sanskrit-for-cbse-parents-guide',
    'A comprehensive comparison to help CBSE parents make an informed decision about their child''s second language — Hindi or Sanskrit.',
    '<h2>The Big Decision</h2><p>Every year, parents of students entering middle school face this crucial choice: should my child study Hindi or Sanskrit as their second language? Both have distinct advantages, and the right choice depends on several individual factors.</p><h2>Hindi: The Practical Choice</h2><p>Hindi is India''s most widely spoken language. Choosing Hindi means your child develops communication skills useful in everyday life, future careers, and understanding a massive body of modern literature and media.</p><h2>Sanskrit: The Academic Powerhouse</h2><p>Sanskrit is often considered an easier scoring subject due to its logical grammar structure. Students who excel in pattern recognition and systematic learning often thrive in Sanskrit. It also builds a strong foundation for learning multiple Indian languages.</p><h2>Key Factors to Consider</h2><ul><li><strong>Prior Exposure:</strong> If your child already speaks Hindi at home, scoring will be easier, but learning may feel less stimulating. Sanskrit could offer a fresh intellectual challenge.</li><li><strong>Career Goals:</strong> For students interested in linguistics, Indology, or civil services, Sanskrit provides unique advantages.</li><li><strong>Scoring Potential:</strong> Both subjects offer excellent scoring potential with proper preparation. Sanskrit''s structured grammar can lead to more predictable exam outcomes.</li><li><strong>Continuation:</strong> Consider whether the school offers the chosen subject through higher classes.</li></ul><h2>Our Recommendation</h2><p>There is no universally right answer. The best choice aligns with your child''s interests, learning style, and long-term academic plans. At BrightNest Academy, we offer expert tuition in both Hindi and Sanskrit to help your child excel regardless of their choice.</p>',
    'LANGUAGE_INSIGHTS',
    'BrightNest Academy',
    TRUE,
    '2026-02-01 14:00:00'
),
(
    'Building Strong Reading Habits in Children: Tips for Parents',
    'building-strong-reading-habits-in-children-tips-for-parents',
    'Reading is the foundation of all learning. Here are practical strategies parents can use to cultivate a lifelong love of reading in their children.',
    '<h2>Why Reading Matters</h2><p>Children who read regularly perform better across all academic subjects — not just languages. Reading develops vocabulary, comprehension, critical thinking, and imagination simultaneously.</p><h2>Start with Their Interests</h2><p>Do not force children to read what you think they should read. Let them choose books about topics they love — whether it is dinosaurs, space, fairy tales, or sports. The goal is to make reading feel like a pleasure, not a chore.</p><h2>Create a Reading Environment</h2><p>Designate a comfortable, well-lit reading corner at home. Keep books accessible — on shelves at their height, on the bedside table, in the car. When books are everywhere, reading becomes natural.</p><h2>Read Together</h2><p>Even after children can read independently, shared reading time remains valuable. Take turns reading aloud, discuss stories, and ask open-ended questions about characters and plot.</p><h2>Lead by Example</h2><p>Children mirror their parents. If they see you reading regularly — whether books, magazines, or newspapers — they naturally understand that reading is a valued activity.</p><h2>Set Realistic Goals</h2><p>Start with just 15 minutes of reading time daily. Gradually increase as the habit strengthens. Celebrate milestones — finishing a book, reading for 30 consecutive days, or discovering a favorite author.</p><h3>We Can Help</h3><p>At BrightNest Academy, our language tuition programs integrate reading comprehension development into every lesson. We help students not just read, but truly understand and enjoy what they read.</p>',
    'LEARNING_TIPS',
    'BrightNest Academy',
    TRUE,
    '2026-01-15 10:30:00'
)
ON DUPLICATE KEY UPDATE title=title;

-- =====================================================
-- Useful Queries for Testing
-- =====================================================

-- View all users
-- SELECT * FROM users;

-- View all courses
-- SELECT * FROM courses;

-- View all enrollments with details
-- SELECT e.id, u.name as student_name, c.title as course_title, e.enrolled_at, e.status
-- FROM enrollments e
-- JOIN users u ON e.user_id = u.id
-- JOIN courses c ON e.course_id = c.id;

-- Count enrollments per course
-- SELECT c.title, COUNT(e.id) as total_enrollments
-- FROM courses c
-- LEFT JOIN enrollments e ON c.id = e.course_id
-- GROUP BY c.id, c.title;
