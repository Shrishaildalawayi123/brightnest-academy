# SaaS Backend Architecture, Security Audit, and Delivery Roadmap

## 1) Architecture Design (Clean Architecture + Spring Boot)

### Suggested layer model

- `domain` (entities, domain services, business rules)
- `application` (use-cases/services, DTO mapping, transactions)
- `infrastructure` (JPA repositories, external integrations, Redis, SMTP, storage)
- `api` (controllers, request/response DTOs, exception handlers)
- `security` (JWT, filters, auth providers, authorization)

### Current implementation mapping

- Entities: `src/main/java/com/shrishailacademy/model`
- Repositories: `src/main/java/com/shrishailacademy/repository`
- Services: `src/main/java/com/shrishailacademy/service`
- Controllers: `src/main/java/com/shrishailacademy/controller`
- DTOs: `src/main/java/com/shrishailacademy/dto` and `src/main/java/com/shrishailacademy/dto/response`
- Security: `src/main/java/com/shrishailacademy/security`
- Tenant context: `src/main/java/com/shrishailacademy/tenant`

### Multi-tenant design

- Tenant context source: `X-Tenant-ID` header or JWT `tenantId` claim.
- Tenant-aware queries: repository methods with `...AndTenantId(...)`.
- Tenant entity: `src/main/java/com/shrishailacademy/model/Tenant.java`.
- Request-scoped tenant storage: `TenantContext`.

## 2) Full CRUD Module (Course)

Implemented and production-usable:

- Entity: `src/main/java/com/shrishailacademy/model/Course.java`
- DTOs: `CourseCreateRequest`, `CourseUpdateRequest`, `CourseResponse`
- Repository: `CourseRepository`
- Service: `CourseService`
- Controller: `CourseController`
- Validation: Jakarta annotations on DTOs
- Exception handling: global mapping via `GlobalExceptionHandler`

## 3) Secure JWT Authentication

Implemented:

- Login endpoint: `POST /api/auth/login`
- Token generation/validation: `JwtTokenProvider`
- Filter: `JwtAuthenticationFilter`
- Security config: `SecurityConfig`
- Role-based authorization: endpoint rules + `@PreAuthorize`

Hardening applied in this pass:

- Refresh access token now always uses tenant from persisted user, not ambient context.
- Tenant header/token mismatch is rejected when JWT is present.

## 4) Spring Security Audit (findings)

### High

- Cross-tenant header ambiguity with JWT present (fixed): header and token tenant can no longer diverge silently.

### Medium

- Actuator exposure in non-prod includes metrics/prometheus. Keep admin-only and enforce network restrictions.
- CSP allows `'unsafe-inline'` for scripts/styles. Consider nonce-based CSP for stricter protection.

### Low

- `X-XSS-Protection` header is legacy; modern browsers ignore it. Keep CSP as primary XSS defense.

## 5) OWASP Top 10 review

### SQL injection

- Status: low risk. Repository queries are JPQL/derived queries with parameter binding.
- Keep avoiding dynamic string concatenation for SQL.

### XSS

- Status: mitigated by input sanitization and security headers.
- Residual risk: rendering rich HTML content in frontend without strict sanitization rules.

### Auth bypass

- Status: improved with tenant/JWT consistency checks and role checks.

### Insecure deserialization

- Status: no obvious Java native deserialization use found.

### Missing security headers

- Status: mostly covered in `SecurityConfig` + `SecurityHeaderFilter`.

## 6) API Validation

- Request DTOs already use `@NotNull`, `@NotBlank`, `@Email`, `@Size` in key APIs.
- Validation failures return structured JSON through `GlobalExceptionHandler`.

## 7) Exception Handling

Implemented in `GlobalExceptionHandler`:

- Access denied
- Validation exceptions
- Method not allowed
- Resource not found
- Generic server errors

Response shape:

- timestamp
- status
- error
- message

## 8) Performance Optimization findings

- Potential N+1 risk around lazy relationships in list endpoints if DTO mapping touches nested lazy fields.
- Indexing is good in JPA entity annotations, but SQL bootstrap schema needs full multi-tenant alignment.
- Rate limiter now supports Redis backend for distributed consistency.

## 9) Database Schema Optimization findings

Critical gap:

- `database/schema.sql` is not fully aligned with current multi-tenant JPA model (`tenant_id` across domain tables).

Recommendation:

- Move to migration-based schema (Flyway/Liquibase).
- Add composite indexes by tenant + frequently queried columns.
- Enforce FKs with `tenant_id` consistency where applicable.

## 10) Integration Tests

Existing suites already cover:

- Auth flows
- Authorization status codes
- Validation and exception mapping
- CSRF
- Rate limiting

Added in this pass:

- `src/test/java/com/shrishailacademy/integration/TenantSecurityIntegrationTest.java`

## 11) Rate Limiting (Bucket4j + Redis)

Implemented:

- API policy: 100 req/min
- Login policy: 5 attempts/min
- 429 responses with `Retry-After`
- Backends: in-memory and Redis-capable

## 12) Monitoring (Actuator/Prometheus/Grafana)

Implemented baseline:

- Actuator + Prometheus dependencies present.
- Endpoints configured in `application.properties`.

Next:

- Add Grafana dashboard JSON and alert rules in `deploy/`.

## 13) Logging System

Implemented:

- Request logging filter
- Error logging through handler/services
- Audit event logging with `AuditLogService`

Next:

- Add JSON structured layout in `logback-spring.xml` for easier SIEM ingestion.

## 14) CI/CD Pipeline

Already present:

- `.github/workflows/ci-cd.yml`
- Build/test/security scan/docker build/deploy stages

Next hardening:

- Add dependency vulnerability scanning gate.
- Add SBOM generation and container image signing.

## 15) Docker Deployment

Current `Dockerfile` is production-lean:

- Multi-stage build
- Non-root runtime
- Healthcheck
- JRE base image

## 16) AWS Deployment

AWS deployment docs exist:

- `deploy/aws/README.md`
- Nginx reverse proxy config included

## 17) SaaS Multi-Tenant Design

Implemented core mechanics:

- Tenant entity + context + filter + tenant-aware repository patterns

Priority next step:

- Complete schema and data migration strategy for strict tenant isolation at DB level.

## 18) Repository Code Review Summary

Main strengths:

- Good coverage of auth/security filters.
- Clear service/controller separation.
- Strong integration-test footprint.

Main concerns:

- Schema drift risk between SQL bootstrap and JPA model.
- Some modules still mix transport/data concerns and can be further decoupled.

## 19) Refactoring Guidance (SOLID/Clean)

- Introduce mapper layer for DTO/entity conversion.
- Split large services by use case (command/query separation).
- Replace ad-hoc bootstrap SQL with migrations.
- Introduce ports/adapters for external dependencies.

## 20) Full System Audit Roadmap

### Phase 1 (now)

- Fix tenant/JWT consistency and refresh-token tenant issuance.
- Keep CI green with targeted security tests.

### Phase 2

- Align DB schema with entities using Flyway.
- Add migration tests and rollback checks.

### Phase 3

- Strengthen CSP and security response headers.
- Add SAST + dependency/CVE scan as blocking checks in CI.

### Phase 4

- Performance pass (slow-query logs, endpoint profiling, query plans).
- Add tenant-level performance dashboards in Grafana.

### Phase 5

- Architecture cleanup into explicit domain/application/infrastructure packages.
- Add contract tests for all public APIs.
