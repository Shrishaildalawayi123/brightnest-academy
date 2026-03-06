package com.shrishailacademy.service;

import com.shrishailacademy.model.AuditLog;
import com.shrishailacademy.repository.AuditLogRepository;
import com.shrishailacademy.tenant.TenantContext;
import com.shrishailacademy.util.InputSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Audit Log Service - Records security-relevant events asynchronously.
 * 
 * Note: tenantId is passed explicitly to @Async methods because
 * ThreadLocal-based TenantContext is not propagated to async executor threads.
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Record an audit event asynchronously to avoid slowing down request
     * processing. tenantId must be resolved on the calling thread.
     */
    @Async
    @Transactional
    public void logEvent(Long tenantId, Long userId, String action, String details, HttpServletRequest request) {
        try {
            String ipAddress = extractClientIp(request);
            String userAgent = request.getHeader("User-Agent");

            AuditLog auditLog = new AuditLog();
            auditLog.setTenantId(tenantId);
            auditLog.setUserId(userId);
            auditLog.setAction(InputSanitizer.sanitizeAndTruncate(action, 100));
            auditLog.setDetails(InputSanitizer.sanitizeAndTruncateNullable(details, 500));
            auditLog.setIpAddress(InputSanitizer.sanitizeAndTruncateNullable(ipAddress, 45));
            auditLog.setUserAgent(InputSanitizer.sanitizeAndTruncateNullable(userAgent, 500));

            auditLogRepository.save(auditLog);
            log.debug("Audit logged: action={} userId={} ip={}", action, userId, ipAddress);
        } catch (Exception e) {
            // Audit logging must never break the main flow
            log.error("Failed to write audit log: action={} error={}", action, e.getMessage());
        }
    }

    /**
     * Record an audit event without an HTTP request context.
     * tenantId must be resolved on the calling thread.
     */
    @Async
    @Transactional
    public void logEvent(Long tenantId, Long userId, String action, String details, String ipAddress) {
        try {
            AuditLog auditLog = new AuditLog(
                    tenantId,
                    userId,
                    InputSanitizer.sanitizeAndTruncate(action, 100),
                    InputSanitizer.sanitizeAndTruncateNullable(details, 500),
                    InputSanitizer.sanitizeAndTruncateNullable(ipAddress, 45));
            auditLogRepository.save(auditLog);
            log.debug("Audit logged: action={} userId={}", action, userId);
        } catch (Exception e) {
            log.error("Failed to write audit log: action={} error={}", action, e.getMessage());
        }
    }

    /**
     * Retrieve audit logs paginated (admin only).
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        return auditLogRepository.findByTenantIdOrderByTimestampDesc(tenantId, pageable);
    }

    /**
     * Retrieve audit logs for a specific user.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByUser(Long userId, Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        return auditLogRepository.findByTenantIdAndUserId(tenantId, userId, pageable);
    }

    /**
     * Retrieve audit logs by action type.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByAction(String action, Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        return auditLogRepository.findByTenantIdAndAction(tenantId, action, pageable);
    }

    /**
     * Extract real client IP, respecting reverse proxy headers.
     */
    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // First IP in X-Forwarded-For is the original client
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

}
