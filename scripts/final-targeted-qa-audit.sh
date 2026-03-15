#!/usr/bin/env bash

# BrightNest Final Targeted QA + DB Validation
# Run on EC2 Ubuntu host from repository root:
#   chmod +x scripts/final-targeted-qa-audit.sh
#   ./scripts/final-targeted-qa-audit.sh

set -u
set -o pipefail

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-brightnest}"
DB_USER="${DB_USER:-brightnest}"
DB_PASS="${DB_PASS:-BrightNest@123}"
APP_SERVICE="${APP_SERVICE:-brightnest}"
DOMAIN="${DOMAIN:-brightnest-academy.com}"

CONTACT_TEST_EMAIL="testuser@example.com"
TEACHER_TEST_EMAIL="teacher@test.com"

CONTACT_FORM="FAIL"
CAREERS_FORM="FAIL"
ADMIN_DASHBOARD="FAIL"
STUDENT_DASHBOARD="FAIL"
VISITOR_TRACKING="FAIL"
FOOTER_PAGES="FAIL"
DATABASE_INTEGRITY="FAIL"

MYSQL_BASE=(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "-p$DB_PASS" "$DB_NAME" -N -B)

run_sql() {
  local sql="$1"
  "${MYSQL_BASE[@]}" -e "$sql"
}

print_section() {
  local title="$1"
  printf "\n==================================================\n"
  printf "%s\n" "$title"
  printf "==================================================\n"
}

safe_cmd() {
  local desc="$1"
  shift
  printf "\n[RUN] %s\n" "$desc"
  if "$@"; then
    return 0
  fi
  printf "[WARN] Command failed: %s\n" "$desc"
  return 1
}

http_code() {
  local method="$1"
  local url="$2"
  local body="${3:-}"

  if [[ -n "$body" ]]; then
    curl -sS -o /tmp/qa_resp_$$.txt -w "%{http_code}" -X "$method" "$url" \
      -H "Content-Type: application/json" \
      --data "$body"
  else
    curl -sS -o /tmp/qa_resp_$$.txt -w "%{http_code}" -X "$method" "$url"
  fi
}

print_section "STEP 0 - SERVICE AND DB CONNECTION"

safe_cmd "systemctl status mysql" sudo systemctl status mysql --no-pager || true
safe_cmd "systemctl status ${APP_SERVICE}" sudo systemctl status "$APP_SERVICE" --no-pager || true

if run_sql "SELECT DATABASE();" >/tmp/qa_db.txt 2>/tmp/qa_db_err.txt; then
  printf "[PASS] DB connection successful\n"
  cat /tmp/qa_db.txt
else
  printf "[FAIL] DB connection failed\n"
  cat /tmp/qa_db_err.txt
  printf "\nFINAL STATUS: BLOCKED (Database connection failed)\n"
  exit 1
fi

print_section "STEP 1 - APPLY COMPATIBILITY SQL (if present)"
if [[ -f "database/compatibility-qa-views.sql" ]]; then
  if mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "-p$DB_PASS" < database/compatibility-qa-views.sql; then
    printf "[PASS] Applied database/compatibility-qa-views.sql\n"
  else
    printf "[WARN] Could not apply compatibility SQL (continuing).\n"
  fi
else
  printf "[INFO] compatibility SQL file not found, skipping\n"
fi

print_section "STEP 2 - VERIFY REQUIRED TABLES"
run_sql "SHOW TABLES;" | tee /tmp/qa_tables.txt
for t in contact_messages teacher_applications users courses enrollments visitor_logs; do
  if run_sql "SHOW TABLES LIKE '$t';" | grep -q "^$t$"; then
    printf "[PASS] table exists: %s\n" "$t"
  else
    printf "[WARN] table missing: %s\n" "$t"
  fi
done

print_section "STEP 3 - CONTACT FORM DATABASE VALIDATION"
CONTACT_PAYLOAD='{"name":"Test User","email":"testuser@example.com","phone":"9876543210","subject":"General Inquiry","message":"Testing contact form database insertion"}'
CONTACT_HTTP=$(http_code "POST" "https://${DOMAIN}/api/contact" "$CONTACT_PAYLOAD" || true)
printf "Contact API HTTP: %s\n" "$CONTACT_HTTP"
head -c 300 /tmp/qa_resp_$$.txt; printf "\n"

run_sql "SELECT id,name,email,phone,subject,status,created_at,tenant_id FROM contact_messages ORDER BY created_at DESC LIMIT 10;" | tee /tmp/qa_contact_rows.txt
CONTACT_ROWS=$(run_sql "SELECT COUNT(*) FROM contact_messages WHERE email='${CONTACT_TEST_EMAIL}' AND message='Testing contact form database insertion';" 2>/dev/null || echo 0)

if [[ "$CONTACT_HTTP" =~ ^2[0-9][0-9]$ ]] && [[ "${CONTACT_ROWS:-0}" -ge 1 ]]; then
  CONTACT_FORM="PASS"
fi

print_section "STEP 4 - CAREERS / TEACHER APPLICATION VALIDATION"
TEACHER_PAYLOAD='{"fullName":"Demo Teacher","email":"teacher@test.com","phone":"9998887776","subjectExpertise":"Mathematics","qualification":"B.Ed","city":"Bangalore","teachingMode":"ONLINE","experience":"5 years","motivation":"Applying for mathematics teacher position"}'
TEACHER_HTTP=$(http_code "POST" "https://${DOMAIN}/api/teacher-applications" "$TEACHER_PAYLOAD" || true)
printf "Teacher API HTTP: %s\n" "$TEACHER_HTTP"
head -c 300 /tmp/qa_resp_$$.txt; printf "\n"

run_sql "SELECT id, full_name AS name, email, phone, subject_expertise AS subject, experience, created_at, tenant_id FROM teacher_applications ORDER BY created_at DESC LIMIT 10;" | tee /tmp/qa_teacher_rows.txt
TEACHER_ROWS=$(run_sql "SELECT COUNT(*) FROM teacher_applications WHERE email='${TEACHER_TEST_EMAIL}';" 2>/dev/null || echo 0)
run_sql "SELECT COUNT(*) AS total_teacher_applications FROM teacher_applications;"

if [[ "$TEACHER_HTTP" =~ ^2[0-9][0-9]$ ]] && [[ "${TEACHER_ROWS:-0}" -ge 1 ]]; then
  CAREERS_FORM="PASS"
fi

print_section "STEP 5 - ADMIN DASHBOARD DATA VALIDATION"
TOTAL_CONTACTS=$(run_sql "SELECT COUNT(*) FROM contact_messages;" 2>/dev/null || echo "")
TOTAL_TEACHER_APPS=$(run_sql "SELECT COUNT(*) FROM teacher_applications;" 2>/dev/null || echo "")
TOTAL_STUDENTS=$(run_sql "SELECT COUNT(*) FROM users WHERE role='STUDENT';" 2>/dev/null || echo "")
TOTAL_TEACHERS=$(run_sql "SELECT COUNT(*) FROM users WHERE role='TEACHER';" 2>/dev/null || echo "")
TOTAL_COURSES=$(run_sql "SELECT COUNT(*) FROM courses;" 2>/dev/null || echo "")
TOTAL_ENROLLMENTS=$(run_sql "SELECT COUNT(*) FROM enrollments;" 2>/dev/null || echo "")

printf "total_contacts=%s\n" "$TOTAL_CONTACTS"
printf "total_teacher_applications=%s\n" "$TOTAL_TEACHER_APPS"
printf "total_students=%s\n" "$TOTAL_STUDENTS"
printf "total_teachers=%s\n" "$TOTAL_TEACHERS"
printf "total_courses=%s\n" "$TOTAL_COURSES"
printf "total_enrollments=%s\n" "$TOTAL_ENROLLMENTS"

if [[ -n "$TOTAL_CONTACTS" && -n "$TOTAL_TEACHER_APPS" && -n "$TOTAL_STUDENTS" && -n "$TOTAL_TEACHERS" && -n "$TOTAL_COURSES" && -n "$TOTAL_ENROLLMENTS" ]]; then
  ADMIN_DASHBOARD="PASS"
fi

print_section "STEP 6 - STUDENT DASHBOARD DATA VALIDATION"
run_sql "SELECT id,name,email,role,created_at FROM users WHERE role='STUDENT' LIMIT 10;" | tee /tmp/qa_students.txt
run_sql "SELECT u.name,c.title FROM enrollments e JOIN users u ON e.user_id=u.id JOIN courses c ON e.course_id=c.id LIMIT 10;" | tee /tmp/qa_student_courses.txt

STUDENT_COUNT=$(run_sql "SELECT COUNT(*) FROM users WHERE role='STUDENT';" 2>/dev/null || echo 0)
ENROLL_LINK_COUNT=$(run_sql "SELECT COUNT(*) FROM enrollments e JOIN users u ON e.user_id=u.id JOIN courses c ON e.course_id=c.id;" 2>/dev/null || echo 0)

if [[ "${STUDENT_COUNT:-0}" -ge 1 ]] && [[ "${ENROLL_LINK_COUNT:-0}" -ge 1 ]]; then
  STUDENT_DASHBOARD="PASS"
fi

print_section "STEP 7 - VISITOR TRACKING VALIDATION"
if run_sql "SELECT id, session_id, page_url, referrer, ip_address, created_at FROM visitor_logs ORDER BY id DESC LIMIT 10;" | tee /tmp/qa_visitors.txt; then
  VISITOR_TOTAL=$(run_sql "SELECT COUNT(*) FROM visitor_logs;" 2>/dev/null || echo 0)
  printf "total_visitors=%s\n" "$VISITOR_TOTAL"
  VISITOR_TRACKING="PASS"
else
  printf "[WARN] visitor_logs query failed\n"
fi

print_section "STEP 8 - DATABASE RELATIONSHIP INTEGRITY CHECK"
BROKEN_COUNT=$(run_sql "SELECT COUNT(*) FROM enrollments e LEFT JOIN users u ON e.user_id=u.id LEFT JOIN courses c ON e.course_id=c.id WHERE u.id IS NULL OR c.id IS NULL;" 2>/dev/null || echo 9999)
run_sql "SELECT e.id,u.name,c.title FROM enrollments e LEFT JOIN users u ON e.user_id=u.id LEFT JOIN courses c ON e.course_id=c.id WHERE u.id IS NULL OR c.id IS NULL;" || true
printf "broken_relationship_rows=%s\n" "$BROKEN_COUNT"
if [[ "${BROKEN_COUNT:-9999}" -eq 0 ]]; then
  DATABASE_INTEGRITY="PASS"
fi

print_section "STEP 9 - FINAL QA SUMMARY QUERY"
run_sql "SELECT (SELECT COUNT(*) FROM contact_messages) AS contact_messages, (SELECT COUNT(*) FROM teacher_applications) AS teacher_applications, (SELECT COUNT(*) FROM users WHERE role='STUDENT') AS students, (SELECT COUNT(*) FROM users WHERE role='TEACHER') AS teachers, (SELECT COUNT(*) FROM courses) AS courses, (SELECT COUNT(*) FROM enrollments) AS enrollments, (SELECT COUNT(*) FROM visitor_logs) AS visitors;"

print_section "STEP 10 - FOOTER PAGE HTTP VALIDATION"
ABOUT_CODE=$(curl -sS -o /dev/null -w "%{http_code}" "https://${DOMAIN}/about")
PRIVACY_CODE=$(curl -sS -o /dev/null -w "%{http_code}" "https://${DOMAIN}/privacy")
CONTACT_CODE=$(curl -sS -o /dev/null -w "%{http_code}" "https://${DOMAIN}/contact")
printf "/about   -> %s\n" "$ABOUT_CODE"
printf "/privacy -> %s\n" "$PRIVACY_CODE"
printf "/contact -> %s\n" "$CONTACT_CODE"

if [[ "$ABOUT_CODE" == "200" && "$PRIVACY_CODE" == "200" && "$CONTACT_CODE" == "200" ]]; then
  FOOTER_PAGES="PASS"
fi

print_section "STEP 11 - NGINX ERROR CHECK"
if safe_cmd "tail nginx error log" sudo tail -n 50 /var/log/nginx/error.log | tee /tmp/qa_nginx_error.log; then
  if grep -Eiq "\b500\b|\b502\b|upstream|no live upstreams|connect\(\) failed" /tmp/qa_nginx_error.log; then
    printf "[WARN] Potential nginx/application errors found in error.log\n"
  else
    printf "[PASS] No 500/502/upstream failure patterns in tail output\n"
  fi
else
  printf "[WARN] Could not read nginx error log\n"
fi

print_section "STEP 12 - FINAL QA RESULT REPORT"
printf "CONTACT FORM: %s\n" "$CONTACT_FORM"
printf "CAREERS FORM: %s\n" "$CAREERS_FORM"
printf "ADMIN DASHBOARD: %s\n" "$ADMIN_DASHBOARD"
printf "STUDENT DASHBOARD: %s\n" "$STUDENT_DASHBOARD"
printf "VISITOR TRACKING: %s\n" "$VISITOR_TRACKING"
printf "FOOTER PAGES: %s\n" "$FOOTER_PAGES"
printf "DATABASE INTEGRITY: %s\n" "$DATABASE_INTEGRITY"

if [[ "$CONTACT_FORM" == "PASS" && "$CAREERS_FORM" == "PASS" && "$ADMIN_DASHBOARD" == "PASS" && "$STUDENT_DASHBOARD" == "PASS" && "$VISITOR_TRACKING" == "PASS" && "$FOOTER_PAGES" == "PASS" && "$DATABASE_INTEGRITY" == "PASS" ]]; then
  printf "\nFINAL STATUS: PRODUCTION READY\n"
else
  printf "\nFINAL STATUS: NEEDS ATTENTION\n"
fi
