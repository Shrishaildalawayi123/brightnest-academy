package com.shrishailacademy.service;

import com.shrishailacademy.model.AuditLog;
import com.shrishailacademy.repository.AuditLogRepository;
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
 * Actions tracked:
 * - LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT
 * - REGISTER, PASSWORD_CHANGE
 * - PAYMENT_INITIATED, PAYMENT_CONFIRMED, PAYMENT_FAILED
 * - COURSE_CREATED, COURSE_UPDATED, COURSE_DELETED
 * - ADMIN_ACCESS
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
     * processing.
     */
    @Async
    @Transactional
    public void logEvent(Long userId, String action, String details, HttpServletRequest request) {
        try {
            String ipAddress = extractClientIp(request);
            String userAgent = request.getHeader("User-Agent");

            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setAction(action);
            auditLog.setDetails(details);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent != null ? truncate(userAgent, 500) : null);

            auditLogRepository.save(auditLog);
            log.debug("Audit logged: action={} userId={} ip={}", action, userId, ipAddress);
        } catch (Exception e) {
            // Audit logging must never break the main flow
            log.error("Failed to write audit log: action={} error={}", action, e.getMessage());
        }
    }

    /**
     * Record an audit event without an HTTP request context.
     */
    @Async
    @Transactional
    public void logEvent(Long userId, String action, String details, String ipAddress) {
        try {
            AuditLog auditLog = new AuditLog(userId, action, details, ipAddress);
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
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }

    /**
     * Retrieve audit logs for a specific user.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    /**
     * Retrieve audit logs by action type.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
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

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
