-- Data consistency checks

-- 1) Payments must reference existing enrollment when enrollment_id is not null
SELECT p.id, p.enrollment_id
FROM payments p
LEFT JOIN enrollments e ON e.id = p.enrollment_id
WHERE p.enrollment_id IS NOT NULL
  AND e.id IS NULL;

-- 2) Attendance should belong to valid users/courses
SELECT a.id
FROM attendance a
LEFT JOIN users u ON u.id = a.user_id
LEFT JOIN courses c ON c.id = a.course_id
WHERE u.id IS NULL OR c.id IS NULL;

-- 3) Published blog posts should have published_at set
SELECT id, title, published, published_at
FROM blog_posts
WHERE published = true
  AND published_at IS NULL;

-- 4) Enrollment/payment mismatch (paid enrollments without any paid payment)
SELECT e.id AS enrollment_id, e.user_id, e.course_id
FROM enrollments e
LEFT JOIN payments p
  ON p.enrollment_id = e.id
 AND p.status = 'SUCCESS'
WHERE p.id IS NULL;
