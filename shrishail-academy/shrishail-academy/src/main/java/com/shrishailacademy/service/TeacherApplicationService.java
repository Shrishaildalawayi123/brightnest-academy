package com.shrishailacademy.service;

import com.shrishailacademy.dto.TeacherApplicationRequest;
import com.shrishailacademy.exception.BusinessException;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.TeacherApplication;
import com.shrishailacademy.repository.TeacherApplicationRepository;
import com.shrishailacademy.tenant.TenantContext;
import com.shrishailacademy.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class TeacherApplicationService {

    private static final Logger log = LoggerFactory.getLogger(TeacherApplicationService.class);

    private final TeacherApplicationRepository teacherApplicationRepository;
    private final TenantService tenantService;

    public TeacherApplicationService(TeacherApplicationRepository teacherApplicationRepository,
                                     TenantService tenantService) {
        this.teacherApplicationRepository = teacherApplicationRepository;
        this.tenantService = tenantService;
    }

    @Transactional
    public TeacherApplication submitApplication(TeacherApplicationRequest request) {
        return submitApplication(request, null, null);
    }

    @Transactional
    public TeacherApplication submitApplication(TeacherApplicationRequest request,
            String resumeFileName, String resumeFilePath) {
        TeacherApplication application = new TeacherApplication();
        application.setTenant(tenantService.requireCurrentTenant());
        application.setFullName(InputSanitizer.sanitizeAndTruncate(request.getFullName(), 100));
        application.setEmail(InputSanitizer.sanitizeEmailAndTruncate(request.getEmail(), 100));
        application.setPhone(InputSanitizer.sanitizeAndTruncate(request.getPhone(), 20));
        application.setSubjectExpertise(InputSanitizer.sanitizeAndTruncate(request.getSubjectExpertise(), 200));
        application.setQualification(InputSanitizer.sanitizeAndTruncateNullable(request.getQualification(), 200));
        application.setCity(InputSanitizer.sanitizeAndTruncateNullable(request.getCity(), 100));
        application.setTeachingMode(InputSanitizer.sanitizeAndTruncateNullable(request.getTeachingMode(), 20));
        application.setExperience(InputSanitizer.sanitizeAndTruncateNullable(request.getExperience(), 500));
        application.setMotivation(InputSanitizer.sanitizeAndTruncateNullable(request.getMotivation(), 1000));
        application.setResumeFileName(InputSanitizer.sanitizeAndTruncateNullable(resumeFileName, 255));
        application.setResumeFilePath(InputSanitizer.sanitizeAndTruncateNullable(resumeFilePath, 500));
        application.setStatus(TeacherApplication.Status.NEW);

        TeacherApplication saved = teacherApplicationRepository.save(application);
        log.info("TEACHER_APPLICATION_SUBMITTED: id={}, name='{}', email='{}', mode='{}', resume='{}'",
                saved.getId(), saved.getFullName(), saved.getEmail(),
                saved.getTeachingMode(), saved.getResumeFileName() != null ? saved.getResumeFileName() : "none");
        return saved;
    }

    public List<TeacherApplication> getAllApplications(String status) {
        Long tenantId = TenantContext.requireTenantId();
        if (status != null && !status.isEmpty()) {
            try {
                TeacherApplication.Status s = TeacherApplication.Status.valueOf(status.toUpperCase());
                return teacherApplicationRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, s);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid status", "INVALID_STATUS");
            }
        }
        return teacherApplicationRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional
    public TeacherApplication updateStatus(Long id, String status) {
        Long tenantId = TenantContext.requireTenantId();
        TeacherApplication app = teacherApplicationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("TeacherApplication", "id", id));

        try {
            TeacherApplication.Status newStatus = TeacherApplication.Status.valueOf(status.toUpperCase());
            app.setStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid status value", "INVALID_STATUS");
        }

        TeacherApplication saved = teacherApplicationRepository.save(app);
        log.info("TEACHER_APPLICATION_STATUS_UPDATED: id={}, status={}", id, status);
        return saved;
    }

    public Map<String, Object> getStats() {
        Long tenantId = TenantContext.requireTenantId();
        return Map.of(
                "total", teacherApplicationRepository.countByTenantId(tenantId),
                "new", teacherApplicationRepository.countByTenantIdAndStatus(tenantId, TeacherApplication.Status.NEW),
                "reviewed", teacherApplicationRepository.countByTenantIdAndStatus(tenantId, TeacherApplication.Status.REVIEWED),
                "contacted", teacherApplicationRepository.countByTenantIdAndStatus(tenantId, TeacherApplication.Status.CONTACTED),
                "hired", teacherApplicationRepository.countByTenantIdAndStatus(tenantId, TeacherApplication.Status.HIRED));
    }
}
