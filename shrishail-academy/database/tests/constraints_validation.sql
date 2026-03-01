-- Constraint behavior validation

-- 1) Detect duplicate enrollments that should be blocked by unique key
SELECT user_id, course_id, COUNT(*) AS duplicates
FROM enrollments
GROUP BY user_id, course_id
HAVING COUNT(*) > 1;

-- 2) Detect duplicate attendance entries for same user/course/date
SELECT user_id, course_id, attendance_date, COUNT(*) AS duplicates
FROM attendance
GROUP BY user_id, course_id, attendance_date
HAVING COUNT(*) > 1;

-- 3) Verify foreign key consistency for enrollments
SELECT e.id
FROM enrollments e
LEFT JOIN users u ON u.id = e.user_id
LEFT JOIN courses c ON c.id = e.course_id
WHERE u.id IS NULL OR c.id IS NULL;

-- 4) Verify foreign key consistency for payments
SELECT p.id
FROM payments p
LEFT JOIN users u ON u.id = p.user_id
LEFT JOIN courses c ON c.id = p.course_id
WHERE u.id IS NULL OR c.id IS NULL;
