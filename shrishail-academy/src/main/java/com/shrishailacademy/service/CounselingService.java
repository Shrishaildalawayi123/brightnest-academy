package com.shrishailacademy.service;

import com.shrishailacademy.dto.CounselingRequestDTO;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.CounselingRequest;
import com.shrishailacademy.repository.CounselingRequestRepository;
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

    public CounselingService(CounselingRequestRepository counselingRepo) {
        this.counselingRepo = counselingRepo;
    }

    /**
     * Save a new counseling callback request from the home page form.
     */
    @Transactional
    public CounselingRequest submitRequest(CounselingRequestDTO dto) {
        CounselingRequest req = new CounselingRequest();
        req.setStudentName(dto.getStudentName());
        req.setStudentClass(dto.getStudentClass());
        req.setBoard(dto.getBoard());
        req.setParentPhone(dto.getParentPhone());
        req.setStatus(CounselingRequest.Status.NEW);

        CounselingRequest saved = counselingRepo.save(req);
        log.info("COUNSELING_REQUEST: student='{}' class='{}' board='{}' phone='{}'",
                dto.getStudentName(), dto.getStudentClass(), dto.getBoard(), "REDACTED");
        return saved;
    }

    /**
     * Admin: get all counseling requests ordered by newest first.
     */
    public List<CounselingRequest> getAllRequests() {
        return counselingRepo.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Admin: get only new/uncontacted requests.
     */
    public List<CounselingRequest> getNewRequests() {
        return counselingRepo.findByStatusOrderByCreatedAtDesc(CounselingRequest.Status.NEW);
    }

    /**
     * Admin: update the status of a counseling request.
     */
    @Transactional
    public CounselingRequest updateStatus(Long id, String status) {
        CounselingRequest req = counselingRepo.findById(id)
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
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", counselingRepo.count());
        stats.put("new", counselingRepo.countByStatus(CounselingRequest.Status.NEW));
        stats.put("contacted", counselingRepo.countByStatus(CounselingRequest.Status.CONTACTED));
        stats.put("completed", counselingRepo.countByStatus(CounselingRequest.Status.COMPLETED));
        return stats;
    }
}
