package com.shrishailacademy.service;

import com.shrishailacademy.dto.TeacherApplicationRequest;
import com.shrishailacademy.exception.BusinessException;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.TeacherApplication;
import com.shrishailacademy.repository.TeacherApplicationRepository;
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

    public TeacherApplicationService(TeacherApplicationRepository teacherApplicationRepository) {
        this.teacherApplicationRepository = teacherApplicationRepository;
    }

    @Transactional
    public TeacherApplication submitApplication(TeacherApplicationRequest request) {
        return submitApplication(request, null, null);
    }

    @Transactional
    public TeacherApplication submitApplication(TeacherApplicationRequest request,
            String resumeFileName, String resumeFilePath) {
        TeacherApplication application = new TeacherApplication();
        application.setFullName(request.getFullName());
        application.setEmail(request.getEmail());
        application.setPhone(request.getPhone());
        application.setSubjectExpertise(request.getSubjectExpertise());
        application.setQualification(request.getQualification());
        application.setCity(request.getCity());
        application.setTeachingMode(request.getTeachingMode());
        application.setExperience(request.getExperience());
        application.setMotivation(request.getMotivation());
        application.setResumeFileName(resumeFileName);
        application.setResumeFilePath(resumeFilePath);
        application.setStatus(TeacherApplication.Status.NEW);

        TeacherApplication saved = teacherApplicationRepository.save(application);
        log.info("TEACHER_APPLICATION_SUBMITTED: id={}, name='{}', email='{}', mode='{}', resume='{}'",
                saved.getId(), request.getFullName(), request.getEmail(),
                request.getTeachingMode(), resumeFileName != null ? resumeFileName : "none");
        return saved;
    }

    public List<TeacherApplication> getAllApplications(String status) {
        if (status != null && !status.isEmpty()) {
            try {
                TeacherApplication.Status s = TeacherApplication.Status.valueOf(status.toUpperCase());
                return teacherApplicationRepository.findByStatusOrderByCreatedAtDesc(s);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid status", "INVALID_STATUS");
            }
        }
        return teacherApplicationRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public TeacherApplication updateStatus(Long id, String status) {
        TeacherApplication app = teacherApplicationRepository.findById(id)
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
        return Map.of(
                "total", teacherApplicationRepository.count(),
                "new", teacherApplicationRepository.countByStatus(TeacherApplication.Status.NEW),
                "reviewed", teacherApplicationRepository.countByStatus(TeacherApplication.Status.REVIEWED),
                "contacted", teacherApplicationRepository.countByStatus(TeacherApplication.Status.CONTACTED),
                "hired", teacherApplicationRepository.countByStatus(TeacherApplication.Status.HIRED));
    }
}
