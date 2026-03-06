package com.shrishailacademy.service;

import com.shrishailacademy.dto.TestimonialRequest;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.Testimonial;
import com.shrishailacademy.repository.TestimonialRepository;
import com.shrishailacademy.tenant.TenantContext;
import com.shrishailacademy.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TestimonialService {

    private static final Logger log = LoggerFactory.getLogger(TestimonialService.class);

    private final TestimonialRepository testimonialRepo;
    private final TenantService tenantService;

    public TestimonialService(TestimonialRepository testimonialRepo, TenantService tenantService) {
        this.testimonialRepo = testimonialRepo;
        this.tenantService = tenantService;
    }

    public List<Testimonial> getApprovedTestimonials() {
        Long tenantId = TenantContext.requireTenantId();
        return testimonialRepo.findByTenantIdAndApprovedTrueOrderByCreatedAtDesc(tenantId);
    }

    public List<Testimonial> getAllTestimonials() {
        Long tenantId = TenantContext.requireTenantId();
        return testimonialRepo.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional
    public Testimonial addTestimonial(TestimonialRequest request) {
        Testimonial testimonial = new Testimonial();
        testimonial.setTenant(tenantService.requireCurrentTenant());
        testimonial.setStudentName(InputSanitizer.sanitizeAndTruncate(request.getStudentName(), 100));
        testimonial.setCourseName(InputSanitizer.sanitizeAndTruncateNullable(request.getCourseName(), 100));
        testimonial.setReview(InputSanitizer.sanitizeAndTruncate(request.getReview(), 1000));
        testimonial.setRating(Math.max(1, Math.min(5, request.getRating())));
        testimonial.setApproved(false);
        Testimonial saved = testimonialRepo.save(testimonial);
        log.info("TESTIMONIAL_ADDED: id={}, name='{}'", saved.getId(), saved.getStudentName());
        return saved;
    }

    @Transactional
    public Testimonial toggleApproval(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Testimonial t = testimonialRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Testimonial", "id", id));
        t.setApproved(!t.isApproved());
        Testimonial saved = testimonialRepo.save(t);
        log.info("TESTIMONIAL_TOGGLED: id={}, approved={}", id, t.isApproved());
        return saved;
    }

    @Transactional
    public void deleteTestimonial(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        if (!testimonialRepo.existsByIdAndTenantId(id, tenantId)) {
            throw new ResourceNotFoundException("Testimonial", "id", id);
        }
        testimonialRepo.deleteByIdAndTenantId(id, tenantId);
        log.info("TESTIMONIAL_DELETED: id={}", id);
    }
}
