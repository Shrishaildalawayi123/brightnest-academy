package com.shrishailacademy.service;

import com.shrishailacademy.dto.CounselingRequestDTO;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.CounselingRequest;
import com.shrishailacademy.repository.CounselingRequestRepository;
import com.shrishailacademy.tenant.TenantContext;
import com.shrishailacademy.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class CounselingService {

    private static final Logger log = LoggerFactory.getLogger(CounselingService.class);

    private final CounselingRequestRepository counselingRepo;
    private final TenantService tenantService;

    public CounselingService(CounselingRequestRepository counselingRepo, TenantService tenantService) {
        this.counselingRepo = counselingRepo;
        this.tenantService = tenantService;
    }

    /**
     * Save a new counseling callback request from the home page form.
     */
    @Transactional
    public CounselingRequest submitRequest(CounselingRequestDTO dto) {
        CounselingRequest req = new CounselingRequest();
        req.setTenant(tenantService.requireCurrentTenant());
        req.setStudentName(InputSanitizer.sanitizeAndTruncate(dto.getStudentName(), 100));
        req.setStudentClass(InputSanitizer.sanitizeAndTruncate(dto.getStudentClass(), 30));
        req.setBoard(InputSanitizer.sanitizeAndTruncate(dto.getBoard(), 50));
        req.setParentPhone(InputSanitizer.sanitizeAndTruncate(dto.getParentPhone(), 20));
        req.setStatus(CounselingRequest.Status.NEW);

        CounselingRequest saved = counselingRepo.save(req);
        log.info("COUNSELING_REQUEST: student='{}' class='{}' board='{}' phone='{}'",
                saved.getStudentName(), saved.getStudentClass(), saved.getBoard(), "REDACTED");
        return saved;
    }

    /**
     * Admin: get all counseling requests ordered by newest first.
     */
    public List<CounselingRequest> getAllRequests() {
        Long tenantId = TenantContext.requireTenantId();
        return counselingRepo.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    /**
     * Admin: get only new/uncontacted requests.
     */
    public List<CounselingRequest> getNewRequests() {
        Long tenantId = TenantContext.requireTenantId();
        return counselingRepo.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, CounselingRequest.Status.NEW);
    }

    /**
     * Admin: update the status of a counseling request.
     */
    @Transactional
    public CounselingRequest updateStatus(Long id, String status) {
        Long tenantId = TenantContext.requireTenantId();
        CounselingRequest req = counselingRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("CounselingRequest", "id", id));

        try {
            req.setStatus(CounselingRequest.Status.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid status: " + status + ". Valid values: NEW, CONTACTED, COMPLETED, CANCELLED");
        }

        CounselingRequest updated = counselingRepo.save(req);
        log.info("COUNSELING_STATUS_UPDATED: id={} newStatus='{}'", id, status.toUpperCase());
        return updated;
    }

    /**
     * Admin: get stats for counseling requests.
     */
    public Map<String, Object> getStats() {
        Long tenantId = TenantContext.requireTenantId();
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", counselingRepo.countByTenantId(tenantId));
        stats.put("new", counselingRepo.countByTenantIdAndStatus(tenantId, CounselingRequest.Status.NEW));
        stats.put("contacted", counselingRepo.countByTenantIdAndStatus(tenantId, CounselingRequest.Status.CONTACTED));
        stats.put("completed", counselingRepo.countByTenantIdAndStatus(tenantId, CounselingRequest.Status.COMPLETED));
        return stats;
    }
}
