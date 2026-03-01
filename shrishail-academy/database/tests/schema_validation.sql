-- Schema integrity checks

-- 1) Ensure expected tables exist
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
    'users',
    'courses',
    'enrollments',
    'attendance',
    'payments',
    'contact_messages',
    'testimonials',
    'blog_posts',
    'demo_bookings',
    'teacher_applications'
  )
ORDER BY table_name;

-- 2) Verify primary keys
SELECT table_name, column_name
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE()
  AND constraint_name = 'PRIMARY'
ORDER BY table_name;

-- 3) Verify foreign keys
SELECT
  table_name,
  column_name,
  referenced_table_name,
  referenced_column_name
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE()
  AND referenced_table_name IS NOT NULL
ORDER BY table_name, column_name;

-- 4) Verify unique constraints
SELECT table_name, index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_in_unique_index
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND non_unique = 0
GROUP BY table_name, index_name
ORDER BY table_name, index_name;
