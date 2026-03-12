package com.shrishailacademy.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrishailacademy.dto.ApiErrorResponse;
import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.repository.TenantRepository;
import com.shrishailacademy.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * Resolves tenant for tenant-scoped API endpoints.
 *
 * Resolution order:
 * 1) JWT claim "tenantId" when present
 * 2) Header X-Tenant-ID (tenantKey) for unauthenticated endpoints
 * (login/register/public)
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String DEFAULT_TENANT_KEY = "default";

    private final JwtTokenProvider jwtTokenProvider;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    public TenantContextFilter(JwtTokenProvider jwtTokenProvider,
            TenantRepository tenantRepository,
            ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tenantRepository = tenantRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // If a JWT is present (Authorization header or auth cookie), we must
        // resolve tenant context even for non-API routes (e.g., protected HTML pages).
        String jwt = getJwtFromRequest(request);
        if (StringUtils.hasText(jwt)) {
            return false;
        }

        String path = normalizeApiPath(request.getRequestURI());
        if (!path.startsWith("/api/")) {
            return true;
        }

        // Enforce tenancy on all multi-tenant domain APIs.
        return !(path.startsWith("/api/auth/")
                || path.startsWith("/api/courses")
                || path.startsWith("/api/users")
                || path.startsWith("/api/enrollments")
                || path.startsWith("/api/payments")
                || path.startsWith("/api/attendance")
                || path.startsWith("/api/blog")
                || path.startsWith("/api/contact")
                || path.startsWith("/api/demo-booking")
                || path.startsWith("/api/counseling")
                || path.startsWith("/api/testimonials")
                || path.startsWith("/api/teacher-applications"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                Long tenantId = jwtTokenProvider.getTenantIdFromToken(jwt);
                if (tenantId == null) {
                    writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                            "Token is missing tenant context.");
                    return;
                }

                String tenantKeyHeader = request.getHeader(TENANT_HEADER);
                if (StringUtils.hasText(tenantKeyHeader)) {
                    Optional<Tenant> tenantFromHeader = tenantRepository.findByTenantKey(tenantKeyHeader.trim());
                    if (tenantFromHeader.isEmpty()) {
                        writeError(response, HttpServletResponse.SC_NOT_FOUND, "Not Found", "Tenant not found.");
                        return;
                    }
                    if (!tenantId.equals(tenantFromHeader.get().getId())) {
                        writeError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                                "Tenant header does not match token tenant.");
                        return;
                    }
                }

                TenantContext.set(tenantId, null);
                filterChain.doFilter(request, response);
                return;
            }

            String tenantKey = request.getHeader(TENANT_HEADER);
            if (!StringUtils.hasText(tenantKey)) {
                if (allowDefaultTenantFallback(request)) {
                    Tenant tenant = tenantRepository.findByTenantKey(DEFAULT_TENANT_KEY)
                            .orElseGet(() -> {
                                Tenant created = new Tenant();
                                created.setTenantKey(DEFAULT_TENANT_KEY);
                                created.setName("Default Tenant");
                                return tenantRepository.save(created);
                            });
                    TenantContext.set(tenant.getId(), tenant.getTenantKey());
                    filterChain.doFilter(request, response);
                    return;
                }

                writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Bad Request",
                        "Missing required header: " + TENANT_HEADER);
                return;
            }

            Optional<Tenant> tenantOpt = tenantRepository.findByTenantKey(tenantKey.trim());
            if (tenantOpt.isEmpty()) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "Not Found", "Tenant not found.");
                return;
            }

            Tenant tenant = tenantOpt.get();
            TenantContext.set(tenant.getId(), tenant.getTenantKey());
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean allowDefaultTenantFallback(HttpServletRequest request) {
        String path = normalizeApiPath(request.getRequestURI());
        String method = request.getMethod();

        if (!path.startsWith("/api/")) {
            return false;
        }

        // Auth endpoints must work before any tenant header exists
        // (login/register/refresh/logout).
        if (path.startsWith("/api/auth/")) {
            return true;
        }

        // Public GET endpoints used by the marketing site.
        if ("GET".equalsIgnoreCase(method)) {
            return path.startsWith("/api/courses")
                    || path.startsWith("/api/blog")
                    || path.startsWith("/api/testimonials");
        }

        // Public POST endpoints used by the website.
        if ("POST".equalsIgnoreCase(method)) {
            return path.equals("/api/contact")
                    || path.equals("/api/demo-booking")
                    || path.equals("/api/counseling")
                    || path.equals("/api/teacher-applications");
        }

        return false;
    }

    private String normalizeApiPath(String path) {
        if (path != null && path.startsWith("/api/v1/")) {
            return "/api/" + path.substring("/api/v1/".length());
        }
        if ("/api/v1".equals(path)) {
            return "/api";
        }
        return path;
    }

    private void writeError(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse body = new ApiErrorResponse(Instant.now(), status, error, message);
        objectMapper.writeValue(response.getWriter(), body);
    }

    /**
     * Extract JWT token from Authorization header (Bearer) or HttpOnly cookie.
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            if (StringUtils.hasText(token)
                    && !"null".equalsIgnoreCase(token)
                    && !"undefined".equalsIgnoreCase(token)) {
                return token;
            }
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (com.shrishailacademy.security.CsrfProtectionFilter.AUTH_COOKIE_NAME.equals(cookie.getName())
                        && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
