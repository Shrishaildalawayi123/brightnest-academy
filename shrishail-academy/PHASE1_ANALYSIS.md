# Phase 1 — Production Readiness Analysis (Shrishail Academy)

Context: Java 21 + Spring Boot 3.2.x + Spring Security + JWT + JPA/MySQL.

This report summarizes the main findings from the initial codebase review and the highest-ROI fixes applied during this pass.

## ✅ Fixes applied (this pass)

### 1) DB schema ↔ JPA mapping mismatch (Attendance)

- **Issue**: `attendance.marked_by_id` exists in `database/schema.sql`, but `Attendance` mapped the join column as `marked_by`.
- **Risk**: Production startup failure with `ddl-auto=validate`, or schema drift depending on how migrations are applied.
- **Fix**: Updated mapping to `@JoinColumn(name = "marked_by_id")`.
- **Where**: `src/main/java/com/shrishailacademy/model/Attendance.java`.

### 2) Spring context startup failure (PaymentRepository)

- **Issue**: `PaymentRepository` declared multi-tenant derived queries like `findByTenantIdAndUserId(...)`, but `Payment` has **no** `tenantId` property.
- **Impact**: ApplicationContext fails to load; all integration tests fail.
- **Fix**: Removed tenant-related derived query methods and JPQL queries referencing `p.tenant...`.
- **Where**: `src/main/java/com/shrishailacademy/repository/PaymentRepository.java`.

### 3) HTTP correctness (course creation)

- **Issue**: `POST /api/courses` returned `200 OK` on create.
- **Fix**: Return `201 Created`.
- **Where**: `src/main/java/com/shrishailacademy/controller/CourseController.java`.
- **Tests updated**:
  - `src/test/java/com/shrishailacademy/integration/AuthorizationAndUserCrudIntegrationTest.java`
  - `src/test/java/com/shrishailacademy/integration/XssSanitizationIntegrationTest.java`

## Current security posture (high-level)

Already present in the repo (good foundation):

- Stateless auth with JWT filter; custom 401 entrypoint and JSON 403 handling.
- CORS allowlist behavior (not wildcard with credentials).
- Strong security headers (CSP/HSTS/XFO/etc.).
- Refresh token rotation with hashed-at-rest storage.
- Rate limiting on sensitive endpoints.
- Input sanitization applied broadly in services.

## Findings / gaps to address next

### A) API response contract consistency

- Success payloads tend to use `ApiResponse`, while some security/error paths return slightly different JSON shapes.
- Recommendation: standardize error shape across `GlobalExceptionHandler` + security handlers (401/403/429/CSRF) so clients can parse reliably.

### B) Multi-module “Redis present” warnings in tests

- Spring Data reports Redis module presence; health can degrade to `503` when Redis isn’t reachable.
- This is currently tolerated by tests (`health` accepts 200 or 503). Decide whether Redis is optional or required:
  - Optional: configure health groups / disable Redis health in `test` profile.
  - Required: ensure test environment boots Redis.

### C) Authorization/IDOR review (partial)

- Some endpoints already check ownership (good), but we should confirm this consistently for:
  - payments (read/list), enrollments (cancel/update), attendance (mark/modify), admin “user CRUD”.

### D) DB/index alignment & validation

- Confirm production schema matches entity constraints (unique indexes, FK names), and add missing indexes where query patterns demand them.

## Test status

- Maven integration tests run successfully after the fixes above.

---

If you want, I can proceed with Phase 2 by:

1. standardizing error responses (without changing success DTOs),
2. re-running `mvn test`, and
3. tightening authorization checks where any IDOR gaps remain.
