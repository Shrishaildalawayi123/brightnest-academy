package com.shrishailacademy.service;

import com.shrishailacademy.model.AuditLog;
import com.shrishailacademy.repository.AuditLogRepository;
import com.shrishailacademy.tenant.TenantContext;
import com.shrishailacademy.util.InputSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${audit.trust-forward-headers:false}")
    private boolean trustForwardHeaders;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Record an audit event asynchronously to avoid slowing down request
     * processing. tenantId must be resolved on the calling thread.
     */
    public void logEvent(Long tenantId, Long userId, String action, String details, HttpServletRequest request) {
        String remoteAddr = request != null ? request.getRemoteAddr() : null;
        String xff = request != null ? request.getHeader("X-Forwarded-For") : null;
        String realIp = request != null ? request.getHeader("X-Real-IP") : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;
        String ipAddress = resolveClientIp(remoteAddr, xff, realIp);

        logEvent(tenantId, userId, action, details, ipAddress, userAgent);
    }

    @Async
    @Transactional
    public void logEvent(Long tenantId,
            Long userId,
            String action,
            String details,
            String ipAddress,
            String userAgent) {
        try {
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
        logEvent(tenantId, userId, action, details, ipAddress, null);
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

    private String resolveClientIp(String remoteAddr, String xff, String realIp) {
        if (!trustForwardHeaders || !isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null) {
            return false;
        }
        return remoteAddr.equals("127.0.0.1")
                || remoteAddr.equals("0:0:0:0:0:0:0:1")
                || remoteAddr.equals("::1")
                || remoteAddr.startsWith("10.")
                || remoteAddr.startsWith("192.168.")
                || remoteAddr.matches("172\\.(1[6-9]|2[0-9]|3[0-1])\\..*");
    }
}
