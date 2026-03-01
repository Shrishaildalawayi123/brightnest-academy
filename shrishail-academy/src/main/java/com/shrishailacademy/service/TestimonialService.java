package com.shrishailacademy.service;

import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.Testimonial;
import com.shrishailacademy.repository.TestimonialRepository;
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

    public TestimonialService(TestimonialRepository testimonialRepo) {
        this.testimonialRepo = testimonialRepo;
    }

    public List<Testimonial> getApprovedTestimonials() {
        return testimonialRepo.findByApprovedTrueOrderByCreatedAtDesc();
    }

    public List<Testimonial> getAllTestimonials() {
        return testimonialRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Testimonial addTestimonial(Testimonial testimonial) {
        Testimonial saved = testimonialRepo.save(testimonial);
        log.info("TESTIMONIAL_ADDED: id={}, name='{}'", saved.getId(), saved.getStudentName());
        return saved;
    }

    @Transactional
    public Testimonial toggleApproval(Long id) {
        Testimonial t = testimonialRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Testimonial", "id", id));
        t.setApproved(!t.isApproved());
        Testimonial saved = testimonialRepo.save(t);
        log.info("TESTIMONIAL_TOGGLED: id={}, approved={}", id, t.isApproved());
        return saved;
    }

    @Transactional
    public void deleteTestimonial(Long id) {
        if (!testimonialRepo.existsById(id)) {
            throw new ResourceNotFoundException("Testimonial", "id", id);
        }
        testimonialRepo.deleteById(id);
        log.info("TESTIMONIAL_DELETED: id={}", id);
    }
}
