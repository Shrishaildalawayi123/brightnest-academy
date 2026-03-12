-- Index and query-plan checks

-- 1) Verify indexes used by common read paths
EXPLAIN SELECT * FROM courses ORDER BY id LIMIT 20;

EXPLAIN SELECT * FROM enrollments WHERE user_id = 1 ORDER BY enrolled_at DESC;

EXPLAIN SELECT * FROM payments WHERE user_id = 1 ORDER BY created_at DESC;

EXPLAIN SELECT * FROM blog_posts WHERE published = true ORDER BY published_at DESC LIMIT 10;

-- 2) Surface top expensive statements (MySQL Performance Schema)
-- Requires performance_schema enabled.
SELECT
  DIGEST_TEXT,
  COUNT_STAR,
  ROUND(SUM_TIMER_WAIT / 1000000000000, 2) AS total_exec_seconds,
  ROUND(AVG_TIMER_WAIT / 1000000000, 2) AS avg_exec_ms
FROM performance_schema.events_statements_summary_by_digest
ORDER BY SUM_TIMER_WAIT DESC
LIMIT 20;
